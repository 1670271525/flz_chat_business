package com.flz_chat_business.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenPair {
    private String token;
    private String refreshToken;
    private String expireAt;
}
