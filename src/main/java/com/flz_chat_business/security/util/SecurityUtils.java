package com.flz_chat_business.security.util;

import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.security.model.LoginPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginPrincipal) {
            return ((LoginPrincipal) principal).getUserId();
        }
        throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
    }
}
