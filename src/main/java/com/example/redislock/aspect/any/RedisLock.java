package com.example.redislock.aspect.any;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    String key();

    int timeout() default 600; // 默认超时时间为600秒

    TimeUnit timeUnit() default TimeUnit.SECONDS; // 默认时间单位为秒
}

