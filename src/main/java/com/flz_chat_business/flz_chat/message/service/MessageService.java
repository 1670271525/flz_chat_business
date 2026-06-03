package com.flz_chat_business.flz_chat.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.config.properties.MessageFilterProperties;
import com.flz_chat_business.flz_chat.file.service.MinioPresignService;
import com.flz_chat_business.flz_chat.file.vo.FileDownloadPresignVO;
import com.flz_chat_business.flz_chat.mapper.ChatMessageMapper;
import com.flz_chat_business.flz_chat.mapper.ConversationMapper;
import com.flz_chat_business.flz_chat.mapper.ConversationParticipantMapper;
import com.flz_chat_business.flz_chat.mapper.MessageUserDeleteMapper;
import com.flz_chat_business.flz_chat.message.dto.SendMessageRequest;
import com.flz_chat_business.flz_chat.pojo.entity.ChatMessage;
import com.flz_chat_business.flz_chat.pojo.entity.Conversation;
import com.flz_chat_business.flz_chat.pojo.entity.ConversationParticipant;
import com.flz_chat_business.flz_chat.pojo.entity.MessageUserDelete;
import com.flz_chat_business.mq.config.MqConstants;
import com.flz_chat_business.mq.model.payload.BusinessMsgAckPayload;
import com.flz_chat_business.mq.model.payload.BusinessPersistPayload;
import com.flz_chat_business.mq.model.payload.BusinessUserOnlinePayload;
import com.flz_chat_business.mq.model.payload.ChatMsgSendPayload;
import com.flz_chat_business.mq.producer.ChatMqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.flz_chat_business.common.util.DateTimes;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 消息读写与投递业务服务。
 */
public class MessageService {

    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_IMAGE = 2;
    private static final int MESSAGE_TYPE_AUDIO = 3;
    private static final int MESSAGE_TYPE_VIDEO = 4;
    private static final int MESSAGE_TYPE_FILE = 5;
    private static final int MAX_TEXT_LENGTH = 2000;

    private final ChatMessageMapper chatMessageMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationParticipantMapper participantMapper;
    private final MessageUserDeleteMapper messageUserDeleteMapper;
    private final ChatMqProducer chatMqProducer;
    private final MinioPresignService minioPresignService;
    private final MessageFilterProperties messageFilterProperties;

    @Transactional(rollbackFor = Exception.class)
    /**
     * 发送消息。
     *
     * @param userId 发送者用户ID
     * @param request 发送参数（会话、类型、内容、幂等ID）
     * @return 发送结果
     */
    public Map<String, Object> send(Long userId, SendMessageRequest request) {
        // 新需求：纯文本消息统一由 chat 长连接服务经 MQ 回调持久化，本 HTTP 入口仅处理文件类消息。
        if (request.getType() != null && request.getType() == MESSAGE_TYPE_TEXT) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "纯文本消息需由长连接服务通过 MQ 回调持久化");
        }
        if (!isMediaType(request.getType())) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "HTTP 仅支持图片/语音/视频/文件类型消息");
        }
        ConversationParticipant participant = participantMapper.findOne(request.getConversationId(), userId);
        if (participant == null || participant.getQuit() == 1) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "无会话访问权限");
        }

        ChatMessage existed = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getClientMsgId, request.getClientMsgId())
                .last("LIMIT 1"));
        if (existed != null) {
            return toSendResponse(existed, resolveDownloadPresign(existed));
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(request.getConversationId());
        message.setSenderId(userId);
        message.setType(request.getType());
        message.setContent(request.getContent());
        message.setMediaMeta(request.getMediaMeta());
        message.setIsAgent(0);
        message.setStatus(0);
        message.setClientMsgId(request.getClientMsgId());
        message.setDeleted(0);
        chatMessageMapper.insert(message);
        chatMessageMapper.updateConversationLastMessage(request.getConversationId(), message.getMessageId(), message.getCreatedAt());

        publishSend(message);
        return toSendResponse(message, resolveDownloadPresign(message));
    }

    /**
     * 查询历史消息。
     */
    public List<ChatMessage> history(Long userId, Long conversationId, Long beforeId, Integer size) {
        ConversationParticipant participant = participantMapper.findOne(conversationId, userId);
        if (participant == null || participant.getQuit() == 1) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "无会话访问权限");
        }
        int limit = size == null ? 30 : Math.min(Math.max(size, 1), 50);
        return chatMessageMapper.listHistory(conversationId, beforeId, limit, userId);
    }

    /**
     * 查询会话未读消息。
     */
    public List<ChatMessage> unread(Long userId, Long conversationId) {
        ConversationParticipant participant = participantMapper.findOne(conversationId, userId);
        if (participant == null || participant.getQuit() == 1) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "无会话访问权限");
        }
        Long lastRead = participant.getLastReadMessageId() == null ? 0L : participant.getLastReadMessageId();
        return chatMessageMapper.listUnread(conversationId, lastRead, 200, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 撤回消息。
     */
    public void recall(Long userId, Long messageId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || message.getDeleted() == 1) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "消息不存在");
        }
        if (!userId.equals(message.getSenderId())) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "仅发送者可撤回");
        }
        if (message.getCreatedAt() != null && Duration.between(message.getCreatedAt(), DateTimes.now()).toMinutes() > 2) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "超过2分钟不可撤回");
        }
        message.setDeleted(1);
        chatMessageMapper.updateById(message);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        payload.put("conversationId", message.getConversationId());
        payload.put("operatorId", userId);
        chatMqProducer.publish(MqConstants.RK_CHAT_MSG_RECALL, payload);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 单边删除消息（仅对当前用户隐藏）。
     */
    public void deleteForUser(Long userId, Long messageId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "消息不存在");
        }
        MessageUserDelete exists = messageUserDeleteMapper.selectOne(new LambdaQueryWrapper<MessageUserDelete>()
                .eq(MessageUserDelete::getMessageId, messageId)
                .eq(MessageUserDelete::getUserId, userId)
                .last("LIMIT 1"));
        if (exists == null) {
            MessageUserDelete row = new MessageUserDelete();
            row.setMessageId(messageId);
            row.setUserId(userId);
            messageUserDeleteMapper.insert(row);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 处理长连接服务回调的持久化请求。
     */
    public void persistFromMq(BusinessPersistPayload payload) {
        validateTextPersistPayload(payload);
        ChatMessage existed = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getClientMsgId, payload.getClientMsgId())
                .last("LIMIT 1"));
        if (existed != null) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setConversationId(payload.getConversationId());
        message.setSenderId(payload.getSenderId());
        message.setType(payload.getType());
        message.setContent(payload.getContent());
        message.setMediaMeta(payload.getMediaMeta());
        message.setIsAgent(payload.getIsAgent() == null ? 0 : payload.getIsAgent());
        message.setStatus(0);
        message.setClientMsgId(payload.getClientMsgId());
        message.setDeleted(0);
        chatMessageMapper.insert(message);
        chatMessageMapper.updateConversationLastMessage(payload.getConversationId(), message.getMessageId(), message.getCreatedAt());
        publishSend(message);
    }

    /**
     * 用户上线时回放未读消息。
     */
    public void replayUnreadForUser(BusinessUserOnlinePayload payload) {
        if (payload.getUserId() == null) {
            return;
        }
        List<Map<String, Object>> rows = chatMessageMapper.listAllUnreadForUser(payload.getUserId(), 500);
        Map<Long, List<Map<String, Object>>> grouped = rows.stream().collect(Collectors.groupingBy(o -> ((Number) o.get("conversation_id")).longValue()));
        for (Map.Entry<Long, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> replay = new HashMap<>();
            replay.put("targetUserId", payload.getUserId());
            replay.put("conversationId", entry.getKey());
            replay.put("messages", entry.getValue());
            chatMqProducer.publish(MqConstants.RK_CHAT_MSG_REPLAY, replay);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 处理客户端投递确认。
     */
    public void ackMessage(BusinessMsgAckPayload payload) {
        if (payload.getMessageId() == null) {
            return;
        }
        ChatMessage message = chatMessageMapper.selectById(payload.getMessageId());
        if (message == null) {
            return;
        }
        Conversation conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null && conversation.getType() != null && conversation.getType() == 1) {
            if (message.getStatus() == null || message.getStatus() < 2) {
                message.setStatus(2);
                chatMessageMapper.updateById(message);
            }
        }
    }

    private void publishSend(ChatMessage message) {
        List<Long> users = participantMapper.listActiveUserIds(message.getConversationId());
        if (users == null) {
            users = new ArrayList<>();
        }
        List<Long> receivers = users.stream().filter(uid -> !uid.equals(message.getSenderId())).collect(Collectors.toList());

        ChatMsgSendPayload payload = new ChatMsgSendPayload();
        payload.setMessageId(message.getMessageId());
        payload.setConversationId(message.getConversationId());
        payload.setSenderId(message.getSenderId());
        payload.setReceivers(receivers);
        payload.setType(message.getType());
        payload.setContent(message.getContent());
        payload.setMediaMeta(message.getMediaMeta());
        payload.setIsAgent(message.getIsAgent() == null ? 0 : message.getIsAgent());
        payload.setCreatedAt(DateTimes.formatOffset(message.getCreatedAt()));
        FileDownloadPresignVO downloadPresign = resolveDownloadPresign(message);
        if (downloadPresign != null) {
            payload.setDownloadUrl(downloadPresign.getUrl());
            payload.setDownloadUrlExpireAt(DateTimes.nowZoned()
                    .plusSeconds(downloadPresign.getExpireSeconds())
                    .format(DateTimes.ISO_OFFSET));
        }
        chatMqProducer.publish(MqConstants.RK_CHAT_MSG_SEND, payload);
    }

    private FileDownloadPresignVO resolveDownloadPresign(ChatMessage message) {
        if (message.getType() == null || message.getType() == 1 || message.getContent() == null) {
            return null;
        }
        try {
            return minioPresignService.presignDownload(message.getContent());
        } catch (Exception ex) {
            log.warn("generate download url failed, messageId={}", message.getMessageId(), ex);
            return null;
        }
    }

    private Map<String, Object> toSendResponse(ChatMessage message, FileDownloadPresignVO downloadPresign) {
        Map<String, Object> result = new HashMap<>();
        result.put("messageId", message.getMessageId());
        result.put("conversationId", message.getConversationId());
        result.put("createdAt", message.getCreatedAt());
        result.put("downloadUrl", downloadPresign == null ? null : downloadPresign.getUrl());
        return result;
    }

    /**
     * 校验并过滤 MQ 文本消息。
     * - 仅允许 type=1（纯文本）由 MQ 入库
     * - 校验发送者会话权限
     * - 过滤敏感词并限制长度
     */
    private void validateTextPersistPayload(BusinessPersistPayload payload) {
        if (payload == null) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "MQ payload 不能为空");
        }
        if (payload.getType() == null || payload.getType() != MESSAGE_TYPE_TEXT) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "business.msg.persist 仅支持纯文本消息(type=1)");
        }
        if (payload.getConversationId() == null || payload.getSenderId() == null || StringUtils.isBlank(payload.getClientMsgId())) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "MQ 文本消息缺少必要字段");
        }
        ConversationParticipant participant = participantMapper.findOne(payload.getConversationId(), payload.getSenderId());
        if (participant == null || participant.getQuit() == 1) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "发送者无会话权限");
        }
        String filtered = filterTextContent(payload.getContent());
        payload.setContent(filtered);
    }

    /**
     * 文本消息过滤规则：
     * - 去除首尾空白
     * - 限制最大长度
     * - 根据配置决定是否进行敏感词替换
     */
    private String filterTextContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "文本消息不能为空");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "文本消息长度超过限制");
        }
        if (messageFilterProperties.isEnabled() && messageFilterProperties.getSensitiveWords() != null) {
            for (String sensitiveWord : messageFilterProperties.getSensitiveWords()) {
                if (StringUtils.isNotBlank(sensitiveWord)) {
                    text = text.replace(sensitiveWord, "***");
                }
            }
        }
        return text;
    }

    private boolean isMediaType(Integer type) {
        return type != null && (type == MESSAGE_TYPE_IMAGE || type == MESSAGE_TYPE_AUDIO || type == MESSAGE_TYPE_VIDEO || type == MESSAGE_TYPE_FILE);
    }
}
