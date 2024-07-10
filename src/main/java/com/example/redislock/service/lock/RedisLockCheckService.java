package com.example.redislock.service.lock;


import com.example.redislock.api.lock.ILockable;
import com.example.redislock.api.lock.LockKey;
import com.example.redislock.api.base.Response;
import com.example.redislock.aspect.paramter.RedisLockCheck;
import com.example.redislock.utils.RedisLock;
import com.example.redislock.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Parameter;
import java.time.Duration;

/**
 * 业务类型加锁校验
 */
@Service
@Slf4j
public class RedisLockCheckService {
    /**
     * 超时时间
     */
    public static final int TIME_OUT = 10 * 1000;

    /**
     * 锁前缀
     */
    private static final String LOCK_PREFIX = "order:lock:";

    /**
     * 加锁结果枚举
     */
    private enum LockResult {
        /**
         * 加锁成功
         */
        SUCCESS,

        /**
         * 加锁失败
         */
        FAIL,

        /**
         * 不需要加锁
         */
        NO_NEED
    }

    /**
     * Redis锁，此处用的最简单的版本
     */
    private final RedisLock lock;

    /**
     * Spring加载Bean时会执行此构造函数
     */
    public RedisLockCheckService(StringRedisTemplate strRedis) {
        lock = new RedisLock(Utils.uuidBase64(), strRedis);
    }

    /**
     * 业务流程加锁和解锁
     */
    public Object doLock(ProceedingJoinPoint joinPoint) throws Throwable {
        LockResult lockResult = LockResult.NO_NEED;

        // 判断是否需要加锁，如果需要应返回锁的key
        LockKey key = getLockKey(joinPoint);
        if (null != key) {
            // 加锁失败的直接抛错，不再进行解锁
            boolean result = lock.lock(key.getKey(), Duration.ofMillis(key.getTimeout()));
            lockResult = result ? LockResult.SUCCESS : LockResult.FAIL;
            if (LockResult.FAIL.equals(lockResult)) {
                // NOTE:这里的失败是包括了加锁过程本身的错误，以及该锁已经存在导致无法继续加锁，这个报错是否准确
                log.info("redis order-lock, key is: {}, the result is {}", key.getKey(), result);
                return Response.fail("ResultCode.DUPLICATE_MESSAGE", "该消息已经在处理中！");
            }
        }

        Object result;
        try {
            // 执行原始逻辑
            result = joinPoint.proceed();
        }
        //无论业务执行成功和失败，都要解锁
        finally {
            // 解锁(只有加锁成功之后才需要解锁)
            if (LockResult.SUCCESS.equals(lockResult)) {
                unlock(key.getKey());
            }
        }

        return result;
    }

    private void unlock(String lockKey) {
        boolean unlockResult = lock.unlock(lockKey);
        if (!unlockResult) {
            log.info("redis order-unlock, key is: {}, the result is {}", lockKey, unlockResult);
            log.error("fail to unlock, please wait 10 seconds.  ");
        }
    }

    /**
     * 判断是否需要加锁，需要的话获取锁的Key值
     */
    private LockKey getLockKey(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (null == args || args.length <= 0) {
            return null;
        }

        // 获取方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        if (null == parameters || parameters.length <= 0) {
            return null;
        }

        // 默认只有一个参数
        Object request = args[0];
        if (request == null) {
            return null;
        }

        // 有加锁注解才处理
        Parameter parameter = parameters[0];
        RedisLockCheck lockCheck = parameter.getAnnotation(RedisLockCheck.class);
        if (lockCheck == null) {
            return null;
        }

        // 实现了key值接口的才处理(实现接口只是基本条件，还需要加注解才会加锁)
        if (!(request instanceof ILockable lockable)) {
            return null;
        }

        String key = lockable.getLockKey();
        if (StringUtils.isBlank(key)) {
            return null;
        }

        LockKey res = new LockKey();
        res.setKey(LOCK_PREFIX + key);
        res.setTimeout(lockCheck.timeout());

        return res;
    }
}
