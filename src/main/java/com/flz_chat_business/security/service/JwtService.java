package com.flz_chat_business.security.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.flz_chat_business.common.util.DateTimes;
import com.flz_chat_business.config.properties.JwtProperties;
import com.flz_chat_business.flz_chat.pojo.entity.ChatUser;
import com.flz_chat_business.security.dto.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final JwtTokenStateService jwtTokenStateService;

    public TokenPair issueTokenPair(ChatUser user) {
        Long version = jwtTokenStateService.getUserTokenVersion(user.getUserId());
        Instant now = Instant.now();
        Instant accessExpireAt = now.plusMillis(jwtProperties.getExpireTime());
        Instant refreshExpireAt = now.plusMillis(jwtProperties.getRefreshExpireTime());
        String issuer = jwtProperties.getIssuer();

        String accessToken = JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withIssuer(issuer)
                .withSubject(String.valueOf(user.getUserId()))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(accessExpireAt))
                .withClaim("uid", user.getUserId())
                .withClaim("uname", user.getUserName())
                .withClaim("ver", version)
                .withClaim("typ", "access")
                .sign(getAlgorithm());

        String refreshToken = JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withIssuer(issuer)
                .withSubject(String.valueOf(user.getUserId()))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(refreshExpireAt))
                .withClaim("uid", user.getUserId())
                .withClaim("uname", user.getUserName())
                .withClaim("ver", version)
                .withClaim("typ", "refresh")
                .sign(getAlgorithm());

        return new TokenPair(accessToken, refreshToken,
                DateTimes.formatOffset(LocalDateTime.ofInstant(accessExpireAt, DateTimes.ZONE)));
    }

    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(getAlgorithm()).build();
        DecodedJWT jwt = verifier.verify(token);
        String jti = jwt.getId();
        if (jwtTokenStateService.isBlacklisted(jti)) {
            throw new JWTVerificationException("token blacklisted");
        }
        Long uid = jwt.getClaim("uid").asLong();
        Long ver = jwt.getClaim("ver").asLong();
        Long currentVersion = jwtTokenStateService.getUserTokenVersion(uid);
        if (ver == null || !ver.equals(currentVersion)) {
            throw new JWTVerificationException("token version mismatch");
        }
        return jwt;
    }

    public DecodedJWT decodeWithoutVerify(String token) {
        return JWT.decode(token);
    }

    public long remainSeconds(DecodedJWT jwt) {
        Date expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return 0;
        }
        long remain = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remain, 0);
    }

    public String resolveTokenFromHeader(String authHeader) {
        String prefix = jwtProperties.getTokenPrefix();
        if (authHeader == null || prefix == null || !authHeader.startsWith(prefix)) {
            return null;
        }
        return authHeader.substring(prefix.length()).trim();
    }

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(jwtProperties.getSecret());
    }
}
