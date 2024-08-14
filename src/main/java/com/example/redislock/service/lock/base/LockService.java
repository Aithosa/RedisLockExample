package com.example.redislock.service.lock.base;

import com.example.redislock.utils.RedisLock;
import com.example.redislock.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Concurrent Redis Lock Service
 */
@Slf4j
@Service
public class LockService {
    private static final int LOCK_TIMEOUT = 600;
    public static final String LOCK_PREFIX = "lock:";
    private final RedisLock redisLock;

    /**
     * This constructor is executed when the Spring bean is loaded.
     *
     * @param redisTemplate the Redis template
     */
    public LockService(StringRedisTemplate redisTemplate) {
        redisLock = new RedisLock(Utils.uuidBase64(), redisTemplate);
    }

    /**
     * Lock by key - Simple scenario
     *
     * @param key       the lock key
     * @param maxTimeout the maximum lock timeout
     * @return the result of the lock operation
     */
    public boolean lock(String key, int maxTimeout) {
        log.info("Lock with key {}, maxTimeout {}", key, maxTimeout);

        // If no limit is set, default to 10 minutes
        if (maxTimeout <= 0) {
            maxTimeout = LOCK_TIMEOUT;
        }

        key = LOCK_PREFIX + key;
        boolean r = redisLock.lock(key, Duration.ofSeconds(maxTimeout));
        if (!r) {
            log.error("Lock {} end with result {}", key, r);
        }

        log.info("Lock end with result {}", r);

        return r;
    }

    /**
     * Unlock by key - Simple scenario
     *
     * @param key the lock key
     * @return the result of the unlock operation
     */
    public boolean unlock(String key) {
        log.info("Unlock key {}...", key);

        key = LOCK_PREFIX + key;
        boolean r = redisLock.unlock(key);
        if (!r) {
            log.error("Unlock {} end with result {}", key, r);
        }

        return r;
    }
}
