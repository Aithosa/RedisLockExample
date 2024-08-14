package com.example.redislock.service.lock.base;

import com.example.redislock.utils.RedisLock;
import com.example.redislock.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrent Redis Lock Service
 */
@Slf4j
@Service
public class ComplexLockService {
    private static final int LOCK_TIMEOUT = 60;
    private static final int LOCK_MAX_MIN = 600;
    public static final String LOCK_PREFIX = "lock:";

    private final RedisLock redisLock;

    /**
     * Used to store the lock key and the maximum timeout of the lock.
     * <p> This means that if the time is not updated later, the lock will not refresh indefinitely.
     */
    private final Map<String, LocalDateTime> locks = new ConcurrentHashMap<>();

    /**
     * The constructor is executed when the Spring bean is loaded.
     */
    public ComplexLockService(StringRedisTemplate redisTemplate) {
        redisLock = new RedisLock(Utils.uuidBase64(), redisTemplate);
    }

    /**
     * Example scenario: Lock an order
     *
     * @param orderId lock key
     * @return lock result
     */
    public boolean lockOrder(String orderId) {
        boolean rt = StringUtils.isNotBlank(orderId) && lock("order:" + orderId, LOCK_MAX_MIN);
        log.info("Lock order {} result {}", orderId, rt);
        return rt;
    }

    /**
     * Example scenario: Unlock an order
     *
     * @param orderId lock key
     */
    public void unlockOrder(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return;
        }

        boolean rt = unlock("order:" + orderId);
        log.info("Unlock order {} result {}", orderId, rt);
    }

    /**
     * Lock the resource by key.
     * <p> After a successful lock, the expiration time of the lock is saved to facilitate subsequent renewal of the lock.
     *
     * @param key key
     * @param maxTimeout max lock timeout
     * @return whether it succeeded
     */
    public boolean lock(String key, int maxTimeout) {
        log.info("Lock with key {}, maxTimeout {}", key, maxTimeout);

        if (maxTimeout <= 0) {
            maxTimeout = LOCK_MAX_MIN * 10;
        }

        key = LOCK_PREFIX + key;
        // NOTE: The actual effective time for locking is LOCK_TIMEOUT. This single lock duration cannot be changed externally.
        boolean r = redisLock.lock(key, Duration.ofSeconds(LOCK_TIMEOUT));
        if (r) {
            // If the lock is successful, save the lock key and maximum timeout to locks to check renewals.
            // NOTE: The maximum timeout is not the effective time of the lock.
            locks.put(key, LocalDateTime.now().plusSeconds(maxTimeout));
        } else {
            log.error("Lock {} end with result {}", key, r);
        }

        log.info("Lock end with result {}", r);
        return r;
    }

    /**
     * Lock the resource by key.
     * <p> After a successful lock, the expiration time of the lock is saved to facilitate subsequent renewal of the lock.
     *
     * @param key key
     * @param maxTimeout max lock timeout
     * @return lock result
     */
    public boolean lockWithRetry(String key, int maxTimeout) {
        log.info("Lock with key {}, maxTimeout {}", key, maxTimeout);
        if (maxTimeout <= 0) {
            maxTimeout = LOCK_MAX_MIN * 10;
        }
        key = LOCK_PREFIX + key;
        boolean r = false;
        int retryCount = 3;
        // If the lock fails in a short period, analyze the reasons. Network fluctuations might not improve lock success rate by retries.
        while (retryCount-- > 0) {
            try {
                r = redisLock.lock(key, Duration.ofSeconds(LOCK_TIMEOUT));
                if (r) {
                    locks.put(key, LocalDateTime.now().plusSeconds(maxTimeout));
                    break;
                } else {
                    log.error("Failed to lock {}. Retrying...", key);
                }
            } catch (Exception e) {
                log.error("Exception while trying to lock {}: {}", key, e.getMessage());
            }
        }
        log.info("Lock end with result {}", r);
        return r;
    }

    /**
     * Unlock the resource by key.
     *
     * @param key key
     * @return unlock result
     */
    public boolean unlock(String key) {
        log.info("Unlock key {}...", key);
        /*
           What happens if we remove first and unlocking fails?
           1. If the lock was not added by self – it shouldn't be unlocked by self, removal does not affect.
           2. If the lock was added by self - a removal failure would mean it won't be renewed.
         */
        locks.remove(LOCK_PREFIX + key);
        boolean r = redisLock.unlock(key);
        if (!r) {
            log.error("Unlock {} end with result {}", key, r);
        }
        return r;
    }

    /**
     * Unlock the resource by key.
     *
     * @param key        key
     * @return whether it succeeded
     */
    public boolean unlockWithRetry(String key) {
        log.info("Unlock key {}...", key);
        boolean r = false;
        key = LOCK_PREFIX + key;
        locks.remove(key);
        int retryCount = 3;
        while (retryCount-- > 0) {
            try {
                r = redisLock.unlock(key);
                if (r) {
                    break;
                } else {
                    log.error("Failed to unlock {}. Retrying...", key);
                }
            } catch (Exception e) {
                log.error("Exception while trying to unlock {}: {}", key, e.getMessage());
            }
        }
        if (!r) {
            log.error("Unlock {} end with result {}", key, r);
        }
        return r;
    }

    /**
     * Scheduled task: Refresh the lock expiration time.
     * <p> Locks record the maximum lock timeout. If a lock's task keeps running for a long time without unlocking,
     * and exceeds the maximum timeout, it won't be refreshed anymore.
     * <p> This is by design. Can be changed if specific needs arise.
     * <p> NOTE: Consider the task's run cycle for appropriateness.
     */
    @Scheduled(fixedDelay = 50_000)
    public void refreshLock() {
        Iterator<Map.Entry<String, LocalDateTime>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            // If the lock has expired, remove the record.
            if (entry.getValue().isBefore(LocalDateTime.now())) {
                iterator.remove();
            }
            // If the lock hasn't expired, refresh the lock's expiration time but won't update the lock information.
            redisLock.refreshLockExpire(entry.getKey(), Duration.ofSeconds(LOCK_TIMEOUT));
        }
    }

    /**
     * Refresh the lock expiration time with retry logic.
     * <p> Locks record the maximum lock timeout. If a lock's task keeps running for a long time without unlocking,
     * and exceeds the maximum timeout, it won't be refreshed anymore.
     * <p> This is by design. Can be changed if specific needs arise.
     */
    @Scheduled(fixedDelay = 50_000)
    public void refreshLockWithRetry() {
        Iterator<Map.Entry<String, LocalDateTime>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            // If the lock has expired, remove the record.
            if (entry.getValue().isBefore(LocalDateTime.now())) {
                iterator.remove();
                continue;
            }
            // If the lock hasn't expired, refresh the lock's expiration time.
            int retryCount = 3;
            while (retryCount-- > 0) {
                try {
                    // Lock renewal: The renewal operation merely extends the lock's expiration time but won't update the maximum timeout in locks.
                    // If the number of renewals exceeds the maximum timeout, the lock will still be released.
                    boolean refreshed = redisLock.refreshLockExpire(entry.getKey(), Duration.ofSeconds(LOCK_TIMEOUT));
                    if (refreshed) {
                        break;
                    } else {
                        log.error("Failed to refresh lock {}. Retrying...", entry.getKey());
                    }
                } catch (Exception e) {
                    log.error("Exception while trying to refresh lock {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    /**
     * Unlimited refresh of the lock expiration time.
     * <p> If the task is still running (lock not removed), the task will periodically refresh the lock's expiration time
     * and update the local cache's lock information.
     */
    @Scheduled(fixedDelay = 50_000)
    public void refreshLockWithoutLimit() {
        Iterator<Map.Entry<String, LocalDateTime>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            // If the lock has expired, remove the record.
            if (entry.getValue().isBefore(LocalDateTime.now())) {
                iterator.remove();
                continue;
            }
            // If the lock hasn't expired, refresh the lock's expiration time.
            int retryCount = 3;
            while (retryCount-- > 0) {
                try {
                    boolean refreshed = redisLock.refreshLockExpire(entry.getKey(), Duration.ofSeconds(LOCK_TIMEOUT));
                    if (refreshed) {
                        // NOTE: Update the maximum timeout for the lock.
                        entry.setValue(LocalDateTime.now().plusSeconds(LOCK_TIMEOUT));
                        break;
                    } else {
                        log.error("Failed to refresh lock {}. Retrying...", entry.getKey());
                    }
                } catch (Exception e) {
                    log.error("Exception while trying to refresh lock {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }
}
