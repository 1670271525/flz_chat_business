package com.flz_chat_business.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityBizProperties {

    private Login login = new Login();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Login {
        private Integer maxFail;
        private Integer lockMinutes;
    }

    @Data
    public static class RateLimit {
        private Integer registerPerIpHour;
        private Integer forgotPerEmailHour;
        private Integer sendMsgPerUserMin;
    }
}
