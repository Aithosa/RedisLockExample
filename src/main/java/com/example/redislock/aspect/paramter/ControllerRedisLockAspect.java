package com.example.redislock.aspect.paramter;

import com.example.redislock.service.lock.RedisLockCheckService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 切面，加锁处理
 */
@Aspect
@Component
@Order(99)
@Slf4j
public class ControllerRedisLockAspect {

    @Autowired
    private RedisLockCheckService lockService;

    @Pointcut("execution(* com.example.redislock.*.controller.*Controller.*(..))")
    public void controllerCheckPointcut() {
    }

    /**
     * 业务流程加锁和解锁
     */
    @Around("controllerCheckPointcut()")
    public Object doLock(ProceedingJoinPoint joinPoint) throws Throwable {
        return lockService.doLock(joinPoint);
    }
}
