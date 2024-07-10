package com.example.redislock.service.lock.base;

import com.example.redislock.utils.RedisLock;
import com.example.redislock.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 并发Redis锁
 */
@Slf4j
@Service
public class LockService {
    private static final int LOCK_TIMEOUT = 600;

    public static final String LOCK_PREFIX = "lock:";

    private final RedisLock redisLock;

    /**
     * Spring加载Bean时会执行此构造函数
     */
    public LockService(StringRedisTemplate redisTemplate) {
        redisLock = new RedisLock(Utils.uuidBase64(), redisTemplate);
    }

    /**
     * 根据key加锁-简单场景
     *
     * @param key lock key
     * @param maxTimeout max lock timeout
     * @return lock result
     */
    public boolean lock(String key, int maxTimeout) {
        log.info("Lock with key {}, maxTimeout {}", key, maxTimeout);

        // 不限定时为10分钟
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
     * 根据key解锁-简单场景
     *
     * @param key lock key
     * @return unlock result
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
