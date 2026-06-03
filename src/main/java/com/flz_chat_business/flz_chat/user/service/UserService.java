package com.flz_chat_business.flz_chat.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.common.util.PageUtils;
import com.flz_chat_business.flz_chat.mapper.ChatUserMapper;
import com.flz_chat_business.flz_chat.mapper.FriendshipMapper;
import com.flz_chat_business.flz_chat.mapper.UserInformationMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ChatUser;
import com.flz_chat_business.flz_chat.pojo.entity.Friendship;
import com.flz_chat_business.flz_chat.pojo.entity.UserInformation;
import com.flz_chat_business.flz_chat.user.dto.ChangePasswordRequest;
import com.flz_chat_business.flz_chat.user.dto.UpdateProfileRequest;
import com.flz_chat_business.security.service.JwtTokenStateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * 用户资料与账户业务服务。
 */
public class UserService {

    private final ChatUserMapper chatUserMapper;
    private final UserInformationMapper userInformationMapper;
    private final FriendshipMapper friendshipMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenStateService jwtTokenStateService;

    /**
     * 查询当前用户完整资料。
     *
     * @param userId 当前登录用户ID
     * @return 资料数据
     */
    public Map<String, Object> getMyProfile(Long userId) {
        ChatUser user = getUserOrThrow(userId);
        UserInformation info = userInformationMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("userName", user.getUserName());
        result.put("email", user.getUserEmail());
        result.put("phone", maskPhone(user.getUserPhone()));
        result.put("information", info);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新当前用户资料。
     *
     * @param userId 当前登录用户ID
     * @param request 资料更新参数
     */
    public void updateMyProfile(Long userId, UpdateProfileRequest request) {
        UserInformation info = userInformationMapper.selectById(userId);
        if (info == null) {
            info = new UserInformation();
            info.setUserId(userId);
            info.setMood("NORMAL");
            info.setGender(0);
            userInformationMapper.insert(info);
        }
        if (request.getNickname() != null) {
            info.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            info.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getMood() != null) {
            info.setMood(request.getMood());
        }
        if (request.getSignature() != null) {
            info.setSignature(request.getSignature());
        }
        if (request.getGender() != null) {
            info.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            info.setBirthday(request.getBirthday());
        }
        if (request.getRegion() != null) {
            info.setRegion(request.getRegion());
        }
        userInformationMapper.updateById(info);
    }

    /**
     * 查看其他用户公开资料。
     *
     * @param currentUserId 当前登录用户ID
     * @param userId 目标用户ID
     * @return 公开资料
     */
    public Map<String, Object> getUserPublicProfile(Long currentUserId, Long userId) {
        ChatUser user = getUserOrThrow(userId);
        Friendship block = friendshipMapper.selectOne(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, currentUserId)
                .eq(Friendship::getStatus, 3)
                .last("LIMIT 1"));
        if (block != null) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "对方已拉黑当前用户");
        }
        UserInformation info = userInformationMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("userName", user.getUserName());
        if (info != null) {
            result.put("avatarUrl", info.getAvatarUrl());
            result.put("nickname", info.getNickname());
            result.put("mood", info.getMood());
            result.put("signature", info.getSignature());
            result.put("region", info.getRegion());
        }
        return result;
    }

    /**
     * 搜索用户列表。
     *
     * @param keyword 搜索关键字
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    public PageData<Map<String, Object>> searchUsers(String keyword, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        if (StringUtils.isBlank(keyword)) {
            return PageData.empty(p, s);
        }
        Long total = chatUserMapper.countSearchUsers(keyword);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        List<Map<String, Object>> records = chatUserMapper.searchUsers(keyword, PageUtils.offset(p, s), s);
        return new PageData<>(total, p, s, records);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 修改登录密码并使历史 token 失效。
     *
     * @param userId 当前登录用户ID
     * @param request 旧密码与新密码
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        ChatUser user = getUserOrThrow(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getUserPassword())) {
            throw new BizException(ResponseCodeEnum.USERNAME_OR_PASSWORD_FAIL.getCode(), "旧密码错误");
        }
        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        chatUserMapper.updateById(user);
        jwtTokenStateService.bumpUserTokenVersion(userId);
    }

    private ChatUser getUserOrThrow(Long userId) {
        ChatUser user = chatUserMapper.selectById(userId);
        if (user == null || user.getUserDeleted() == 1) {
            throw new BizException(ResponseCodeEnum.USER_NOT_EXISTS);
        }
        return user;
    }

    private String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
