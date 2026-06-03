package com.flz_chat_business.flz_chat.auth.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.common.redis.RedisCache;
import com.flz_chat_business.config.properties.SecurityBizProperties;
import com.flz_chat_business.flz_chat.auth.dto.LoginRequest;
import com.flz_chat_business.flz_chat.auth.dto.RefreshTokenRequest;
import com.flz_chat_business.flz_chat.auth.dto.RegisterRequest;
import com.flz_chat_business.flz_chat.auth.dto.ResetPasswordRequest;
import com.flz_chat_business.flz_chat.mapper.ChatUserMapper;
import com.flz_chat_business.flz_chat.mapper.UserInformationMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ChatUser;
import com.flz_chat_business.flz_chat.pojo.entity.UserInformation;
import com.flz_chat_business.security.dto.TokenPair;
import com.flz_chat_business.security.service.JwtService;
import com.flz_chat_business.security.service.JwtTokenStateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
/**
 * 鉴权核心业务服务。
 */
public class AuthService {

    private final ChatUserMapper chatUserMapper;
    private final UserInformationMapper userInformationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtTokenStateService jwtTokenStateService;
    private final AuthAssistService authAssistService;
    private final RedisCache redisCache;
    private final SecurityBizProperties securityBizProperties;

    @Transactional(rollbackFor = Exception.class)
    /**
     * 注册账号并返回 token。
     *
     * @param request 注册参数（用户名、邮箱、手机号、密码、验证码）
     * @return token 载荷
     */
    public Map<String, Object> register(RegisterRequest request) {
        if (!authAssistService.verifyEmailCode(request.getEmail(), "REGISTER", request.getEmailCode())) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "邮箱验证码错误");
        }

        if (existsByUserName(request.getUserName())) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "用户名已存在");
        }
        if (existsByEmail(request.getEmail())) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "邮箱已存在");
        }
        if (StringUtils.isNotBlank(request.getPhone()) && existsByPhone(request.getPhone())) {
            throw new BizException(ResponseCodeEnum.CONFLICT.getCode(), "手机号已存在");
        }

        ChatUser user = new ChatUser();
        user.setUserName(request.getUserName());
        user.setUserEmail(request.getEmail());
        user.setUserPhone(StringUtils.trimToNull(request.getPhone()));
        user.setUserPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserStatus(1);
        user.setUserDeleted(0);
        chatUserMapper.insert(user);

        UserInformation info = new UserInformation();
        info.setUserId(user.getUserId());
        info.setNickname(request.getUserName());
        info.setMood("NORMAL");
        info.setGender(0);
        userInformationMapper.insert(info);
        authAssistService.clearEmailCode(request.getEmail(), "REGISTER");

        TokenPair tokenPair = jwtService.issueTokenPair(user);
        return tokenPayload(user.getUserId(), tokenPair);
    }

    /**
     * 登录并返回 token。
     *
     * @param request 登录参数（account/password）
     * @return token 载荷
     */
    public Map<String, Object> login(LoginRequest request) {
        String lockKey = "rl:login_lock:" + request.getAccount();
        if (Boolean.TRUE.equals(redisCache.hasKey(lockKey))) {
            throw new BizException(ResponseCodeEnum.RATE_LIMITED.getCode(), "登录失败次数过多，请稍后重试");
        }

        ChatUser user = chatUserMapper.findByAccount(request.getAccount());
        if (user == null || user.getUserDeleted() == 1 || user.getUserStatus() != 1 ||
                !passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            markLoginFail(request.getAccount());
            throw new BizException(ResponseCodeEnum.USERNAME_OR_PASSWORD_FAIL);
        }

        clearLoginFail(request.getAccount());
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        return tokenPayload(user.getUserId(), tokenPair);
    }

    /**
     * 刷新 token。
     *
     * @param request refreshToken 入参
     * @return token 载荷
     */
    public Map<String, Object> refresh(RefreshTokenRequest request) {
        DecodedJWT jwt = jwtService.verifyToken(request.getRefreshToken());
        if (!"refresh".equals(jwt.getClaim("typ").asString())) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "refreshToken 类型错误");
        }
        Long userId = jwt.getClaim("uid").asLong();
        ChatUser user = chatUserMapper.selectById(userId);
        if (user == null || user.getUserDeleted() == 1) {
            throw new BizException(ResponseCodeEnum.USER_NOT_EXISTS);
        }
        long remain = jwtService.remainSeconds(jwt);
        jwtTokenStateService.blacklistJti(jwt.getId(), Duration.ofSeconds(remain));
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        return tokenPayload(user.getUserId(), tokenPair);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 忘记密码重置。
     *
     * @param request 邮箱、验证码、新密码
     */
    public void resetPassword(ResetPasswordRequest request) {
        if (!authAssistService.verifyEmailCode(request.getEmail(), "RESET_PASSWORD", request.getEmailCode())) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "邮箱验证码错误");
        }
        ChatUser user = chatUserMapper.selectOne(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserEmail, request.getEmail())
                .eq(ChatUser::getUserDeleted, 0)
                .last("LIMIT 1"));
        if (user == null) {
            throw new BizException(ResponseCodeEnum.USER_NOT_EXISTS);
        }
        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        chatUserMapper.updateById(user);
        jwtTokenStateService.bumpUserTokenVersion(user.getUserId());
        authAssistService.clearEmailCode(request.getEmail(), "RESET_PASSWORD");
    }

    /**
     * 登出并加入 token 黑名单。
     *
     * @param authorization Authorization 请求头值
     */
    public void logout(String authorization) {
        String token = jwtService.resolveTokenFromHeader(authorization);
        if (token == null) {
            return;
        }
        DecodedJWT jwt = jwtService.decodeWithoutVerify(token);
        long remain = jwtService.remainSeconds(jwt);
        if (remain > 0) {
            jwtTokenStateService.blacklistJti(jwt.getId(), Duration.ofSeconds(remain));
        }
    }

    /**
     * 发送邮箱验证码（带60秒间隔限制）。
     *
     * @param email 目标邮箱
     * @param scene 场景
     */
    public void sendEmailCode(String email, String scene) {
        String intervalKey = "rl:email_code_interval:" + scene + ":" + email;
        Boolean first = redisCache.setIfAbsent(intervalKey, "1", 60, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            throw new BizException(ResponseCodeEnum.RATE_LIMITED.getCode(), "60秒内请勿重复发送");
        }
        authAssistService.sendEmailCode(email, scene);
    }

    private boolean existsByUserName(String userName) {
        return chatUserMapper.selectCount(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserName, userName)
                .eq(ChatUser::getUserDeleted, 0)) > 0;
    }

    private boolean existsByEmail(String email) {
        return chatUserMapper.selectCount(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserEmail, email)
                .eq(ChatUser::getUserDeleted, 0)) > 0;
    }

    private boolean existsByPhone(String phone) {
        return chatUserMapper.selectCount(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserPhone, phone)
                .eq(ChatUser::getUserDeleted, 0)) > 0;
    }

    private Map<String, Object> tokenPayload(Long userId, TokenPair tokenPair) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("token", tokenPair.getToken());
        map.put("refreshToken", tokenPair.getRefreshToken());
        map.put("expireAt", tokenPair.getExpireAt());
        return map;
    }

    private void markLoginFail(String account) {
        int lockMinutes = securityBizProperties.getLogin().getLockMinutes();
        int maxFail = securityBizProperties.getLogin().getMaxFail();
        String failKey = "rl:login_fail:" + account;
        String lockKey = "rl:login_lock:" + account;
        Long count = redisCache.increment(failKey, 1);
        if (count != null && count == 1) {
            redisCache.expire(failKey, lockMinutes * 60L, TimeUnit.SECONDS);
        }
        if (count != null && count >= maxFail) {
            redisCache.setCacheObject(lockKey, "1", lockMinutes * 60, TimeUnit.SECONDS);
        }
    }

    private void clearLoginFail(String account) {
        redisCache.deleteObject("rl:login_fail:" + account);
        redisCache.deleteObject("rl:login_lock:" + account);
    }
}
