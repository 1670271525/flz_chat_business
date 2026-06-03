package com.flz_chat_business.flz_chat.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
/**
 * 用户注册请求参数。
 */
public class RegisterRequest {
    /** 登录用户名。 */
    @NotBlank
    @Size(min = 3, max = 64)
    private String userName;

    /** 注册邮箱。 */
    @NotBlank
    @Email
    private String email;

    /** 手机号，可选。 */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /** 登录密码（明文入参，服务端加密存储）。 */
    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,32}$", message = "密码需6-32位且包含字母和数字")
    private String password;

    /** 邮箱验证码。 */
    @NotBlank
    private String emailCode;
}
