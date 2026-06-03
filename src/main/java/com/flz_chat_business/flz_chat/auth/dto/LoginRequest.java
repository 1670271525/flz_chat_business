package com.flz_chat_business.flz_chat.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
/**
 * 登录请求参数。
 */
public class LoginRequest {
    /** 登录账号：支持用户名/邮箱/手机号。 */
    @NotBlank
    private String account;

    /** 登录密码。 */
    @NotBlank
    private String password;
}
