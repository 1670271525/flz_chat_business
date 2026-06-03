package com.flz_chat_business.security.service;

import com.flz_chat_business.common.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtTokenStateService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_VERSION_PREFIX = "jwt:userVersion:";
    private final RedisCache redisCache;

    public void blacklistJti(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisCache.setCacheObject(BLACKLIST_PREFIX + jti, "1", (int) ttl.getSeconds(), TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return false;
        }
        Boolean exists = redisCache.hasKey(BLACKLIST_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }

    public Long getUserTokenVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Object version = redisCache.getCacheObject(USER_VERSION_PREFIX + userId);
        if (version instanceof Number) {
            return ((Number) version).longValue();
        }
        return 0L;
    }

    public Long bumpUserTokenVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String key = USER_VERSION_PREFIX + userId;
        Long current = redisCache.increment(key, 1);
        redisCache.expire(key, 30, TimeUnit.DAYS);
        return current == null ? 0L : current;
    }
}
