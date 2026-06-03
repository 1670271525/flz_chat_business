package com.flz_chat_business.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "message.filter")
public class MessageFilterProperties {

    /**
     * 是否开启敏感词过滤。
     */
    private boolean enabled = true;

    /**
     * 敏感词列表。
     */
    private List<String> sensitiveWords = new ArrayList<>();
}
