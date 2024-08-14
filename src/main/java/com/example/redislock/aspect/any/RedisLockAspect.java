package com.example.redislock.aspect.any;

import com.example.redislock.service.lock.base.LockService;
import com.example.redislock.utils.errorinfo.ErrorCodes;
import com.example.redislock.utils.exception.BizError;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect for Redis-based locking.
 * <p> Ensures that methods annotated with {@link RedisLock} are executed with a lock to prevent concurrent access.
 */
@Order(99)
@Aspect
@Component
public class RedisLockAspect {

    @Autowired
    private LockService lockService;

    /**
     * Around advice that applies Redis lock to methods annotated with {@link RedisLock}.
     * If the lock cannot be acquired, a {@link BizError} is thrown.
     *
     * @param joinPoint the join point representing the annotated method
     * @return the result of the method execution
     * @throws Throwable if an error occurs during method execution
     */
    @Around("@annotation(RedisLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RedisLock redisLock = signature.getMethod().getAnnotation(RedisLock.class);

        String key = redisLock.key();
        int timeout = redisLock.timeout();
        TimeUnit timeUnit = redisLock.timeUnit();

        boolean locked = lockService.lock(key, (int) timeUnit.toSeconds(timeout));
        if (!locked) {
            throw new BizError(ErrorCodes.FAIL, "Unable to acquire lock");
        }

        try {
            return joinPoint.proceed();
        } finally {
            lockService.unlock(key);
        }
    }
}