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
 * Business type lock validation service
 */
@Service
@Slf4j
public class RedisLockCheckService {
    /**
     * Timeout duration
     */
    public static final int TIME_OUT = 10 * 1000;

    /**
     * Lock prefix
     */
    private static final String LOCK_PREFIX = "order:lock:";

    /**
     * Lock result enumeration
     */
    private enum LockResult {
        /**
         * Lock acquired successfully
         */
        SUCCESS,
        /**
         * Failed to acquire lock
         */
        FAIL,
        /**
         * No need to acquire lock
         */
        NO_NEED
    }

    /**
     * Redis lock, using the simplest version here
     */
    private final RedisLock lock;

    /**
     * Constructor executed when Spring loads the Bean
     */
    public RedisLockCheckService(StringRedisTemplate strRedis) {
        lock = new RedisLock(Utils.uuidBase64(), strRedis);
    }

    /**
     * Business process lock and unlock
     */
    public Object doLock(ProceedingJoinPoint joinPoint) throws Throwable {
        LockResult lockResult = LockResult.NO_NEED;
        // Determine if locking is needed, if needed the key should be returned
        LockKey key = getLockKey(joinPoint);
        if (key != null) {
            // Failure during lock acquisition will throw an exception, no unlocking will be performed
            boolean result = lock.lock(key.getKey(), Duration.ofMillis(key.getTimeout()));
            lockResult = result ? LockResult.SUCCESS : LockResult.FAIL;
            if (LockResult.FAIL.equals(lockResult)) {
                // NOTE: This failure includes errors during the lock process itself and the inability to acquire lock due to its existence. Is this error accurate?
                log.info("Redis order-lock, key is: {}, the result is {}", key.getKey(), result);
                return Response.fail("ResultCode.DUPLICATE_MESSAGE", "This message is already being processed!");
            }
        }

        Object result;
        try {
            // Execute the original logic
            result = joinPoint.proceed();
        } finally {
            // Unlock regardless of business execution success or failure
            // Unlock (only needed if lock was acquired successfully)
            if (LockResult.SUCCESS.equals(lockResult)) {
                unlock(key.getKey());
            }
        }

        return result;
    }

    private void unlock(String lockKey) {
        boolean unlockResult = lock.unlock(lockKey);
        if (!unlockResult) {
            log.info("Redis order-unlock, key is: {}, the result is {}", lockKey, unlockResult);
            log.error("Fail to unlock, please wait 10 seconds.");
        }
    }

    /**
     * Determine if locking is needed and get the lock key if needed
     */
    private LockKey getLockKey(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length <= 0) {
            return null;
        }
        // Get method parameters
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        if (parameters == null || parameters.length <= 0) {
            return null;
        }
        // Default to only one parameter
        Object request = args[0];
        if (request == null) {
            return null;
        }
        // Process only if the parameter has the RedisLockCheck annotation
        Parameter parameter = parameters[0];
        RedisLockCheck lockCheck = parameter.getAnnotation(RedisLockCheck.class);
        if (lockCheck == null) {
            return null;
        }
        // Process only if the parameter implements the ILockable interface (implementing the interface is the basic condition, additionally it needs the annotation to lock)
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
