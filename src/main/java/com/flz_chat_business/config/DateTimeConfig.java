package com.flz_chat_business.config;

import com.flz_chat_business.common.util.DateTimes;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
public class DateTimeConfig {

    @PostConstruct
    public void initTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(DateTimes.ZONE));
    }
}
