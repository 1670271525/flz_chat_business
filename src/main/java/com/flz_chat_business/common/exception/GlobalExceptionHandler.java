package com.flz_chat_business.common.exception;

import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleValidationException(Exception ex) {
        return Result.fail(ResponseCodeEnum.PARAM_INVALID.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException ex) {
        return Result.fail(ResponseCodeEnum.UNAUTHORIZED.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        return Result.fail(ResponseCodeEnum.FORBIDDEN.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(ResponseCodeEnum.SYSTEM_EXCEPTION.getCode(), ResponseCodeEnum.SYSTEM_EXCEPTION.getMessage());
    }
}
