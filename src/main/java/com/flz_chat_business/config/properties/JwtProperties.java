package com.flz_chat_business.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String header;
    private String tokenPrefix;
    private String secret;
    private String issuer;
    private Long expireTime;
    private Long refreshExpireTime;
}
