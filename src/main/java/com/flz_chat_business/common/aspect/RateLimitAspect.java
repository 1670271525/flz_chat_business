package com.flz_chat_business.common.aspect;

import com.flz_chat_business.common.annotation.RateLimit;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.common.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rl:";
    private final RedisCache redisCache;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        String principal = resolvePrincipal(rateLimit, joinPoint);
        String key = KEY_PREFIX + rateLimit.action() + ":" + principal;
        Long current = redisCache.increment(key, 1);
        if (current != null && current == 1) {
            redisCache.expire(key, rateLimit.window(), rateLimit.unit());
        }
        if (current != null && current > rateLimit.limit()) {
            throw new BizException(ResponseCodeEnum.RATE_LIMITED);
        }
    }

    private String resolvePrincipal(RateLimit rateLimit, JoinPoint joinPoint) {
        if (StringUtils.isNotBlank(rateLimit.principal())) {
            return rateLimit.principal();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StringUtils.isNotBlank(authentication.getName()) && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName();
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            if (request != null) {
                String ip = extractClientIp(request);
                if (StringUtils.isNotBlank(ip)) {
                    return "ip:" + ip;
                }
            }
        }
        return "unknown";
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.isNotBlank(realIp) ? realIp : request.getRemoteAddr();
    }
}
