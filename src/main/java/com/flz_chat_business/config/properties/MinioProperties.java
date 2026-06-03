package com.flz_chat_business.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketPublic;
    private String bucketChat;
    private Integer presignedExpiryDays;
    private String publicBaseUrl;
}
