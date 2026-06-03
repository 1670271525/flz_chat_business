package com.flz_chat_business.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginPrincipal implements Serializable {
    private Long userId;
    private String username;
    private Long tokenVersion;
}
