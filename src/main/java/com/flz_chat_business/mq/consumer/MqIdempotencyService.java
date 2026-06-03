package com.flz_chat_business.mq.consumer;

import com.flz_chat_business.common.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MqIdempotencyService {

    private static final String KEY_PREFIX = "mq:processed:";
    private final RedisCache redisCache;

    public boolean tryMarkProcessed(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return false;
        }
        Boolean success = redisCache.setIfAbsent(KEY_PREFIX + msgId, "1", 24, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }
}
