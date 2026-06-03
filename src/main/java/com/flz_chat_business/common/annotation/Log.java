package com.flz_chat_business.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义Log注解
 */
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String value();
}
