package com.flz_chat_business.security.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.flz_chat_business.security.model.LoginPrincipal;
import com.flz_chat_business.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = jwtService.resolveTokenFromHeader(authHeader);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                DecodedJWT jwt = jwtService.verifyToken(token);
                if (!"access".equals(jwt.getClaim("typ").asString())) {
                    throw new IllegalStateException("invalid token type");
                }
                LoginPrincipal principal = new LoginPrincipal(
                        jwt.getClaim("uid").asLong(),
                        jwt.getClaim("uname").asString(),
                        jwt.getClaim("ver").asLong()
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                log.debug("Ignore invalid jwt: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
