package com.flz_chat_business.flz_chat.auth.controller;

import com.flz_chat_business.common.annotation.RateLimit;
import com.flz_chat_business.flz_chat.auth.dto.EmailCodeRequest;
import com.flz_chat_business.flz_chat.auth.dto.LoginRequest;
import com.flz_chat_business.flz_chat.auth.dto.RefreshTokenRequest;
import com.flz_chat_business.flz_chat.auth.dto.RegisterRequest;
import com.flz_chat_business.flz_chat.auth.dto.ResetPasswordRequest;
import com.flz_chat_business.flz_chat.auth.service.AuthService;
import com.flz_chat_business.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
/**
 * 鉴权业务接口。
 * 提供注册、登录、验证码、刷新令牌、重置密码、登出等能力。
 */
public class AuthController {

    private final AuthService authService;

    @PostMapping("/email-code")
    @RateLimit(action = "auth.email-code", limit = 3, window = 3600)
    /**
     * 发送邮箱验证码。
     *
     * @param request email 为目标邮箱，scene 为业务场景（REGISTER/RESET_PASSWORD）
     * @return 通用成功响应
     */
    public Result<Void> emailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request.getEmail(), request.getScene());
        return Result.success();
    }

    @PostMapping("/logout")
    /**
     * 登出当前账号。
     *
     * @param authorization Authorization 请求头，格式 Bearer token
     * @return 通用成功响应
     */
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success();
    }

    @PostMapping("/register")
    /**
     * 注册新用户并签发 token。
     *
     * @param request 用户名、邮箱、手机号、密码、验证码
     * @return data 中包含 userId/token/refreshToken/expireAt
     */
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    /**
     * 使用账号密码登录。
     *
     * @param request account 支持用户名/邮箱/手机号，password 为登录密码
     * @return data 中包含 userId/token/refreshToken/expireAt
     */
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/refresh")
    /**
     * 刷新访问令牌。
     *
     * @param request refreshToken 为刷新令牌
     * @return data 中包含新 token/refreshToken/expireAt
     */
    public Result<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request));
    }

    @PostMapping("/reset-password")
    /**
     * 忘记密码场景下重置密码。
     *
     * @param request email 为邮箱，emailCode 为验证码，newPassword 为新密码
     * @return 通用成功响应
     */
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }
}
