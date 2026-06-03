package com.flz_chat_business.flz_chat.friend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.common.util.DateTimes;
import com.flz_chat_business.common.util.PageUtils;
import com.flz_chat_business.flz_chat.friend.dto.CreateFriendRequestDTO;
import com.flz_chat_business.flz_chat.friend.dto.UpdateFriendAliasRequest;
import com.flz_chat_business.flz_chat.mapper.ChatUserMapper;
import com.flz_chat_business.flz_chat.mapper.ConversationMapper;
import com.flz_chat_business.flz_chat.mapper.ConversationParticipantMapper;
import com.flz_chat_business.flz_chat.mapper.FriendRequestMapper;
import com.flz_chat_business.flz_chat.mapper.FriendshipMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ChatUser;
import com.flz_chat_business.flz_chat.pojo.entity.Conversation;
import com.flz_chat_business.flz_chat.pojo.entity.ConversationParticipant;
import com.flz_chat_business.flz_chat.pojo.entity.FriendRequest;
import com.flz_chat_business.flz_chat.pojo.entity.Friendship;
import com.flz_chat_business.mq.config.MqConstants;
import com.flz_chat_business.mq.producer.ChatMqProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * 好友关系业务服务。
 */
public class FriendService {

    private final FriendshipMapper friendshipMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final ChatUserMapper chatUserMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationParticipantMapper conversationParticipantMapper;
    private final ChatMqProducer chatMqProducer;

    /**
     * 查询好友列表。
     *
     * @param userId 当前登录用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页好友列表
     */
    public PageData<Map<String, Object>> listFriends(Long userId, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        Long total = friendshipMapper.countFriends(userId);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        List<Map<String, Object>> records = friendshipMapper.listFriends(userId, PageUtils.offset(p, s), s);
        return new PageData<>(total, p, s, records);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 发起好友申请。
     *
     * @param userId 当前登录用户ID
     * @param requestDTO 申请参数（toUserId/remark）
     */
    public void createRequest(Long userId, CreateFriendRequestDTO requestDTO) {
        if (requestDTO.getToUserId().equals(userId)) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "不能添加自己为好友");
        }
        ChatUser target = chatUserMapper.selectById(requestDTO.getToUserId());
        if (target == null || target.getUserDeleted() == 1) {
            throw new BizException(ResponseCodeEnum.USER_NOT_EXISTS);
        }
        Friendship friendship = friendshipMapper.selectOne(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, requestDTO.getToUserId())
                .last("LIMIT 1"));
        if (friendship != null && friendship.getStatus() == 1) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "已是好友");
        }
        Long pendingCount = friendRequestMapper.countPendingWithin(userId, requestDTO.getToUserId(), DateTimes.now().minusDays(7));
        if (pendingCount != null && pendingCount > 0) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "7天内已发起待处理申请");
        }
        FriendRequest entity = new FriendRequest();
        entity.setFromUserId(userId);
        entity.setToUserId(requestDTO.getToUserId());
        entity.setRemark(requestDTO.getRemark());
        entity.setStatus(0);
        entity.setExpireAt(DateTimes.now().plusDays(7));
        friendRequestMapper.insert(entity);

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", entity.getId());
        payload.put("fromUserId", userId);
        payload.put("toUserId", requestDTO.getToUserId());
        payload.put("remark", requestDTO.getRemark());
        chatMqProducer.publish(MqConstants.RK_CHAT_FRIEND_REQUEST, payload);
    }

    /**
     * 查询收到的好友申请。
     */
    public PageData<Map<String, Object>> listIncoming(Long userId, Integer status, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        Long total = friendRequestMapper.countIncoming(userId, status);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        return new PageData<>(total, p, s, friendRequestMapper.listIncoming(userId, status, PageUtils.offset(p, s), s));
    }

    /**
     * 查询发出的好友申请。
     */
    public PageData<Map<String, Object>> listOutgoing(Long userId, Integer status, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        Long total = friendRequestMapper.countOutgoing(userId, status);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        return new PageData<>(total, p, s, friendRequestMapper.listOutgoing(userId, status, PageUtils.offset(p, s), s));
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 同意好友申请并创建/复用单聊会话。
     */
    public Map<String, Object> accept(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || request.getToUserId() == null || !request.getToUserId().equals(userId)) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "好友申请不存在");
        }
        if (request.getStatus() != 0) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "好友申请状态不可处理");
        }
        request.setStatus(1);
        request.setHandledAt(DateTimes.now());
        friendRequestMapper.updateById(request);

        upsertFriendship(request.getFromUserId(), request.getToUserId(), 1);
        upsertFriendship(request.getToUserId(), request.getFromUserId(), 1);

        Long conversationId = conversationMapper.findSingleConversationId(request.getFromUserId(), request.getToUserId());
        if (conversationId == null) {
            Conversation conversation = new Conversation();
            conversation.setType(1);
            conversation.setDissolved(0);
            conversationMapper.insert(conversation);
            conversationId = conversation.getConversationId();
            insertParticipant(conversationId, request.getFromUserId(), 3);
            insertParticipant(conversationId, request.getToUserId(), 3);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("fromUserId", request.getFromUserId());
        payload.put("toUserId", request.getToUserId());
        payload.put("conversationId", conversationId);
        chatMqProducer.publish(MqConstants.RK_CHAT_FRIEND_ACCEPT, payload);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 拒绝好友申请。
     */
    public void reject(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !userId.equals(request.getToUserId())) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "好友申请不存在");
        }
        if (request.getStatus() != 0) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "好友申请状态不可处理");
        }
        request.setStatus(2);
        request.setHandledAt(DateTimes.now());
        friendRequestMapper.updateById(request);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新好友备注。
     */
    public void updateAlias(Long userId, Long friendId, UpdateFriendAliasRequest request) {
        Friendship friendship = friendshipMapper.selectOne(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId)
                .last("LIMIT 1"));
        if (friendship == null) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "好友关系不存在");
        }
        friendship.setAlias(request.getAlias());
        friendshipMapper.updateById(friendship);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 拉黑好友。
     */
    public void block(Long userId, Long friendId) {
        upsertFriendship(userId, friendId, 3);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 取消拉黑。
     */
    public void unblock(Long userId, Long friendId) {
        upsertFriendship(userId, friendId, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 删除好友关系（双向）。
     */
    public void deleteFriend(Long userId, Long friendId) {
        upsertFriendship(userId, friendId, 2);
        upsertFriendship(friendId, userId, 2);
    }

    private void upsertFriendship(Long userId, Long friendId, Integer status) {
        Friendship row = friendshipMapper.selectOne(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId)
                .last("LIMIT 1"));
        if (row == null) {
            row = new Friendship();
            row.setUserId(userId);
            row.setFriendId(friendId);
            row.setStatus(status);
            friendshipMapper.insert(row);
        } else {
            row.setStatus(status);
            friendshipMapper.updateById(row);
        }
    }

    private void insertParticipant(Long conversationId, Long userId, int role) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setQuit(0);
        participant.setMute(0);
        participant.setPinned(0);
        participant.setLastReadMessageId(0L);
        conversationParticipantMapper.insert(participant);
    }
}
