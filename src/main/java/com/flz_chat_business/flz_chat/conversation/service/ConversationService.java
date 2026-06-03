package com.flz_chat_business.flz_chat.conversation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.common.util.PageUtils;
import com.flz_chat_business.flz_chat.conversation.dto.AddMembersRequest;
import com.flz_chat_business.flz_chat.conversation.dto.CreateGroupConversationRequest;
import com.flz_chat_business.flz_chat.conversation.dto.CreateSingleConversationRequest;
import com.flz_chat_business.flz_chat.conversation.dto.MarkReadRequest;
import com.flz_chat_business.flz_chat.conversation.dto.MuteRequest;
import com.flz_chat_business.flz_chat.conversation.dto.PinRequest;
import com.flz_chat_business.flz_chat.conversation.dto.UpdateConversationRequest;
import com.flz_chat_business.flz_chat.conversation.dto.UpdateRoleRequest;
import com.flz_chat_business.flz_chat.mapper.ChatMessageMapper;
import com.flz_chat_business.flz_chat.mapper.ChatUserMapper;
import com.flz_chat_business.flz_chat.mapper.ConversationMapper;
import com.flz_chat_business.flz_chat.mapper.ConversationParticipantMapper;
import com.flz_chat_business.flz_chat.mapper.UserInformationMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ChatMessage;
import com.flz_chat_business.flz_chat.pojo.entity.Conversation;
import com.flz_chat_business.flz_chat.pojo.entity.ConversationParticipant;
import com.flz_chat_business.flz_chat.pojo.entity.UserInformation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * 会话与成员管理业务服务。
 */
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationParticipantMapper participantMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserInformationMapper userInformationMapper;
    private final ChatUserMapper chatUserMapper;

    /**
     * 查询当前用户会话列表。
     */
    public PageData<Map<String, Object>> list(Long userId, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        Long total = conversationMapper.countMyConversations(userId);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        List<Map<String, Object>> rows = conversationMapper.listMyConversations(userId, PageUtils.offset(p, s), s);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long conversationId = asLong(row, "conversation_id", "conversationId");
            Integer type = asInt(row, "type");
            Long lastMessageId = asLong(row, "last_message_id", "lastMessageId");
            Long lastRead = asLong(row, "last_read_message_id", "lastReadMessageId");
            Map<String, Object> vo = new HashMap<>();
            vo.put("conversationId", conversationId);
            vo.put("type", type);
            vo.put("name", row.get("name"));
            vo.put("avatarUrl", row.get("avatar_url"));
            vo.put("lastMessageId", lastMessageId);
            vo.put("pinned", asInt(row, "pinned") == 1);
            vo.put("mute", asInt(row, "mute") == 1);
            long unread = Math.max(0, (lastMessageId == null ? 0 : lastMessageId) - (lastRead == null ? 0 : lastRead));
            vo.put("unreadCount", unread);
            if (lastMessageId != null && lastMessageId > 0) {
                ChatMessage last = chatMessageMapper.selectById(lastMessageId);
                if (last != null) {
                    Map<String, Object> lastMsg = new HashMap<>();
                    lastMsg.put("type", last.getType());
                    lastMsg.put("preview", preview(last.getContent(), last.getType()));
                    lastMsg.put("senderId", last.getSenderId());
                    lastMsg.put("createdAt", last.getCreatedAt());
                    vo.put("lastMessage", lastMsg);
                }
            }
            if (type != null && type == 1) {
                Map<String, Object> peer = resolvePeer(conversationId, userId);
                vo.put("peer", peer);
                if (vo.get("avatarUrl") == null && peer != null) {
                    vo.put("avatarUrl", peer.get("avatarUrl"));
                }
            } else {
                vo.put("peer", null);
            }
            records.add(vo);
        }
        return new PageData<>(total, p, s, records);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建单聊会话。
     */
    public Map<String, Object> createSingle(Long userId, CreateSingleConversationRequest request) {
        Long peer = request.getPeerUserId();
        if (peer.equals(userId)) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "不能和自己创建单聊");
        }
        if (chatUserMapper.selectById(peer) == null) {
            throw new BizException(ResponseCodeEnum.USER_NOT_EXISTS);
        }
        Long exists = conversationMapper.findSingleConversationId(userId, peer);
        if (exists != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", exists);
            return result;
        }
        Conversation conversation = new Conversation();
        conversation.setType(1);
        conversation.setDissolved(0);
        conversationMapper.insert(conversation);
        createParticipant(conversation.getConversationId(), userId, 3);
        createParticipant(conversation.getConversationId(), peer, 3);
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getConversationId());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建群聊会话。
     */
    public Map<String, Object> createGroup(Long userId, CreateGroupConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setType(2);
        conversation.setName(request.getName());
        conversation.setAvatarUrl(request.getAvatarUrl());
        conversation.setOwnerId(userId);
        conversation.setDissolved(0);
        conversation.setMaxMembers(500);
        conversationMapper.insert(conversation);

        Set<Long> members = new HashSet<>();
        members.add(userId);
        if (request.getMemberIds() != null) {
            members.addAll(request.getMemberIds());
        }
        for (Long memberId : members) {
            if (chatUserMapper.selectById(memberId) == null) {
                continue;
            }
            createParticipant(conversation.getConversationId(), memberId, memberId.equals(userId) ? 1 : 3);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getConversationId());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新会话基础信息（名称/头像）。
     */
    public void updateConversation(Long userId, Long conversationId, UpdateConversationRequest request) {
        Conversation conversation = mustGetConversation(conversationId);
        ensureManager(userId, conversation);
        if (request.getName() != null) {
            conversation.setName(request.getName());
        }
        if (request.getAvatarUrl() != null) {
            conversation.setAvatarUrl(request.getAvatarUrl());
        }
        conversationMapper.updateById(conversation);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 添加群成员。
     */
    public void addMembers(Long userId, Long conversationId, AddMembersRequest request) {
        Conversation conversation = mustGetConversation(conversationId);
        ensureManager(userId, conversation);
        if (request.getMemberIds() == null) {
            return;
        }
        for (Long memberId : request.getMemberIds()) {
            ConversationParticipant old = participantMapper.findOne(conversationId, memberId);
            if (old == null) {
                createParticipant(conversationId, memberId, 3);
            } else if (old.getQuit() != null && old.getQuit() == 1) {
                old.setQuit(0);
                participantMapper.updateById(old);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 移除群成员。
     */
    public void removeMember(Long userId, Long conversationId, Long targetUserId) {
        Conversation conversation = mustGetConversation(conversationId);
        ensureManager(userId, conversation);
        if (targetUserId.equals(conversation.getOwnerId())) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "不能移除群主");
        }
        ConversationParticipant participant = participantMapper.findOne(conversationId, targetUserId);
        if (participant == null) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "会话成员不存在");
        }
        participant.setQuit(1);
        participantMapper.updateById(participant);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 当前用户退出会话。
     */
    public void quit(Long userId, Long conversationId) {
        ConversationParticipant participant = participantMapper.findOne(conversationId, userId);
        if (participant == null) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "会话成员不存在");
        }
        participant.setQuit(1);
        participantMapper.updateById(participant);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 解散会话（群主操作）。
     */
    public void dissolve(Long userId, Long conversationId) {
        Conversation conversation = mustGetConversation(conversationId);
        if (!userId.equals(conversation.getOwnerId())) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "仅群主可解散");
        }
        conversation.setDissolved(1);
        conversationMapper.updateById(conversation);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新成员角色。
     */
    public void updateRole(Long userId, Long conversationId, UpdateRoleRequest request) {
        Conversation conversation = mustGetConversation(conversationId);
        if (!userId.equals(conversation.getOwnerId())) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "仅群主可设置管理员");
        }
        if (request.getRole() == null || (request.getRole() != 2 && request.getRole() != 3)) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "role 仅允许2或3");
        }
        ConversationParticipant participant = participantMapper.findOne(conversationId, request.getUserId());
        if (participant == null) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "会话成员不存在");
        }
        participant.setRole(request.getRole());
        participantMapper.updateById(participant);
    }

    /**
     * 查询会话成员分页列表。
     */
    public PageData<Map<String, Object>> members(Long userId, Long conversationId, Integer page, Integer size) {
        ensureMember(userId, conversationId);
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        Long total = participantMapper.countMembers(conversationId);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        List<Map<String, Object>> records = participantMapper.listMembers(conversationId, PageUtils.offset(p, s), s);
        return new PageData<>(total, p, s, records);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 标记会话已读位置。
     */
    public void markRead(Long userId, Long conversationId, MarkReadRequest request) {
        Conversation conversation = mustGetConversation(conversationId);
        ConversationParticipant participant = ensureMember(userId, conversationId);
        Long conversationLast = conversation.getLastMessageId() == null ? 0L : conversation.getLastMessageId();
        Long target = Math.min(request.getLastReadMessageId(), conversationLast);
        Long current = participant.getLastReadMessageId() == null ? 0L : participant.getLastReadMessageId();
        participant.setLastReadMessageId(Math.max(current, target));
        participantMapper.updateById(participant);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 设置会话置顶状态。
     */
    public void updatePin(Long userId, Long conversationId, PinRequest request) {
        ConversationParticipant participant = ensureMember(userId, conversationId);
        participant.setPinned(Boolean.TRUE.equals(request.getPinned()) ? 1 : 0);
        participantMapper.updateById(participant);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 设置会话免打扰状态。
     */
    public void updateMute(Long userId, Long conversationId, MuteRequest request) {
        ConversationParticipant participant = ensureMember(userId, conversationId);
        participant.setMute(Boolean.TRUE.equals(request.getMute()) ? 1 : 0);
        participantMapper.updateById(participant);
    }

    private Map<String, Object> resolvePeer(Long conversationId, Long userId) {
        List<Long> ids = participantMapper.listActiveUserIds(conversationId);
        if (ids == null) {
            return null;
        }
        for (Long uid : ids) {
            if (!uid.equals(userId)) {
                UserInformation info = userInformationMapper.selectById(uid);
                Map<String, Object> peer = new HashMap<>();
                peer.put("userId", uid);
                peer.put("nickname", info == null ? null : info.getNickname());
                peer.put("avatarUrl", info == null ? null : info.getAvatarUrl());
                return peer;
            }
        }
        return null;
    }

    private ConversationParticipant ensureMember(Long userId, Long conversationId) {
        ConversationParticipant participant = participantMapper.findOne(conversationId, userId);
        if (participant == null || participant.getQuit() == 1) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "无会话访问权限");
        }
        return participant;
    }

    private void ensureManager(Long userId, Conversation conversation) {
        if (conversation.getType() != null && conversation.getType() == 1) {
            ensureMember(userId, conversation.getConversationId());
            return;
        }
        ConversationParticipant me = ensureMember(userId, conversation.getConversationId());
        if (!userId.equals(conversation.getOwnerId()) && (me.getRole() == null || me.getRole() > 2)) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "仅群主或管理员可操作");
        }
    }

    private Conversation mustGetConversation(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || conversation.getDissolved() == 1) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "会话不存在");
        }
        return conversation;
    }

    private void createParticipant(Long conversationId, Long userId, int role) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setQuit(0);
        participant.setMute(0);
        participant.setPinned(0);
        participant.setLastReadMessageId(0L);
        participantMapper.insert(participant);
    }

    private String preview(String content, Integer type) {
        if (type == null || type == 1) {
            if (content == null) {
                return "";
            }
            return content.length() > 20 ? content.substring(0, 20) : content;
        }
        switch (type) {
            case 2:
                return "[图片]";
            case 3:
                return "[语音]";
            case 4:
                return "[视频]";
            case 5:
                return "[文件]";
            default:
                return "[消息]";
        }
    }

    private Long asLong(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        return null;
    }

    private Integer asInt(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return 0;
    }
}
