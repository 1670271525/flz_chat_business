package com.flz_chat_business.flz_chat.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
/**
 * 邮箱验证码请求参数。
 */
public class EmailCodeRequest {
    /** 目标邮箱地址。 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 验证码场景：REGISTER 或 RESET_PASSWORD。 */
    @NotBlank(message = "scene不能为空")
    @Pattern(regexp = "REGISTER|RESET_PASSWORD", message = "scene 仅支持 REGISTER/RESET_PASSWORD")
    private String scene;
}
