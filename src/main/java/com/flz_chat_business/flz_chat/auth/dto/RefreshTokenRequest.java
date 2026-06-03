package com.flz_chat_business.flz_chat.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
/**
 * 刷新 token 请求参数。
 */
public class RefreshTokenRequest {
    /** 刷新令牌。 */
    @NotBlank
    private String refreshToken;
}
