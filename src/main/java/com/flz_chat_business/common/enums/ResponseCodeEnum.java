package com.flz_chat_business.common.enums;

public enum ResponseCodeEnum {
    SUCCESS(20000, "success"),
    FAIL(20001, "fail"),
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或Token无效"),
    TOKEN_EXPIRED(40101, "Token已过期"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    RATE_LIMITED(42900, "触发限流"),
    USERNAME_OR_PASSWORD_FAIL(1001, "用户名或密码错误"),
    USER_NOT_EXISTS(1002, "用户不存在"),
    CONFIRM_FAIL(1003, "确认失败"),
    CONFIRM_SUCCESS(1004, "确认成功"),
    SYSTEM_EXCEPTION(500, "服务端异常");

    private final int code;
    private final String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
