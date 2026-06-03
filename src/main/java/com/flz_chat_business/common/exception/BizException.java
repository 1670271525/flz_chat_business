package com.flz_chat_business.common.exception;

import com.flz_chat_business.common.enums.ResponseCodeEnum;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResponseCodeEnum responseCodeEnum) {
        super(responseCodeEnum.getMessage());
        this.code = responseCodeEnum.getCode();
    }
}
