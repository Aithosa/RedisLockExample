package com.example.redislock.aspect.paramter;


import com.example.redislock.service.lock.RedisLockCheckService;

import java.lang.annotation.*;

/**
 * 接口加锁
 * NOTE:作用在方法参数上，因此只适用于有参方法/接口
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLockCheck {
    int timeout() default RedisLockCheckService.TIME_OUT; // 默认10s
}
