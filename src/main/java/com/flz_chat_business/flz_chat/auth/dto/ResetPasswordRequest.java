package com.flz_chat_business.flz_chat.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
/**
 * 忘记密码重置请求参数。
 */
public class ResetPasswordRequest {
    /** 目标邮箱。 */
    @NotBlank
    @Email
    private String email;

    /** 邮箱验证码。 */
    @NotBlank
    private String emailCode;

    /** 新密码。 */
    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,32}$", message = "密码需6-32位且包含字母和数字")
    private String newPassword;
}
