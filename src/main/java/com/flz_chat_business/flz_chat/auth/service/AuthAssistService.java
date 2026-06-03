package com.flz_chat_business.flz_chat.auth.service;

import com.flz_chat_business.common.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 鉴权辅助服务。
 * 负责邮箱验证码生成、缓存、校验与删除。
 */
public class AuthAssistService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisCache redisCache;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送邮箱验证码并写入缓存。
     *
     * @param email 目标邮箱
     * @param scene 场景标识（REGISTER/RESET_PASSWORD）
     */
    public void sendEmailCode(String email, String scene) {
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        redisCache.setCacheObject(buildCodeKey(scene, email), code, 5, TimeUnit.MINUTES);

        try {
            if (from == null || from.trim().isEmpty()) {
                log.warn("Skip sending email code because spring.mail.username is blank");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("FLZ Chat 验证码");
            message.setText("验证码: " + code + "，5分钟内有效。");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Send mail failed, code still cached. email={}", email, ex);
        }
    }

    /**
     * 校验邮箱验证码。
     *
     * @param email 目标邮箱
     * @param scene 场景
     * @param code 用户输入验证码
     * @return true 表示校验通过
     */
    public boolean verifyEmailCode(String email, String scene, String code) {
        Object cached = redisCache.getCacheObject(buildCodeKey(scene, email));
        return cached != null && cached.toString().equals(code);
    }

    /**
     * 删除已使用的验证码。
     *
     * @param email 目标邮箱
     * @param scene 场景
     */
    public void clearEmailCode(String email, String scene) {
        redisCache.deleteObject(buildCodeKey(scene, email));
    }

    private String buildCodeKey(String scene, String email) {
        return "code:" + scene + ":" + email;
    }
}
