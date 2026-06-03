package com.flz_chat_business.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String action();

    int limit();

    int window();

    TimeUnit unit() default TimeUnit.SECONDS;

    String principal() default "";
}
