package com.example.redislock.aspect.paramter;

import com.example.redislock.service.lock.RedisLockCheckService;
import java.lang.annotation.*;

/**
 * Interface Locking
 * <p> NOTE: Applied on method parameters, thus only applicable to methods/interfaces with parameters.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLockCheck {
    int timeout() default RedisLockCheckService.TIME_OUT; // Default 10s
}