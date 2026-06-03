package com.flz_chat_business.flz_chat.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
/**
 * 修改密码请求参数。
 */
public class ChangePasswordRequest {
    /** 旧密码。 */
    @NotBlank
    private String oldPassword;

    /** 新密码。 */
    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,32}$", message = "密码需6-32位且包含字母和数字")
    private String newPassword;
}
