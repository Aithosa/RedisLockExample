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
 * 并发Redis锁
 */
@Slf4j
@Service
public class ComplexLockService {
    private static final int LOCK_TIMEOUT = 60;

    private static final int LOCK_MAX_MIN = 600;

    public static final String LOCK_PREFIX = "lock:";

    private final RedisLock redisLock;

    /**
     * 用于存储锁的Key和锁的最大超时时间
     * 这意味着如果后续不更新时间，不会让锁无限制刷新下去
     */
    private final Map<String, LocalDateTime> locks = new ConcurrentHashMap<>();

    /**
     * Spring加载Bean时会执行此构造函数
     */
    public ComplexLockService(StringRedisTemplate redisTemplate) {
        redisLock = new RedisLock(Utils.uuidBase64(), redisTemplate);
    }

    /**
     * 示例场景：锁住订单
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
     * 示例场景：解锁订单
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
     * 根据Key锁定资源
     * 加锁成功后会保存锁的到期时间以方便后续给锁续期
     *
     * @param key key
     * @param maxTimeout max lock timeout
     * @return 是否成功
     */
    public boolean lock(String key, int maxTimeout) {
        log.info("Lock with key {}, maxTimeout {}", key, maxTimeout);

        if (maxTimeout <= 0) {
            maxTimeout = LOCK_MAX_MIN * 10;
        }

        key = LOCK_PREFIX + key;
        // NOTE: 实际加锁的有效时间用的LOCK_TIMEOUT，此处锁的单次有效时间外界无法更改
        boolean r = redisLock.lock(key, Duration.ofSeconds(LOCK_TIMEOUT));
        if (r) {
            // 加锁成功则把锁的键以及最大超时时间保存到locks中，方便检查续期
            // NOTE:注意最大超时时间不是锁的有效时间
            locks.put(key, LocalDateTime.now().plusSeconds(maxTimeout));
        } else {
            log.error("Lock {} end with result {}", key, r);
        }

        log.info("Lock end with result {}", r);
        return r;
    }

    /**
     * 根据Key锁定资源
     * 加锁成功后会保存锁的到期时间以方便后续给锁续期
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

        // 短时间内加锁失败需要分析具体是什么原因导致的，如果是网络波动重试有可能不会提高加锁成功率
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
     * 根据Key解锁资源
     *
     * @param key key
     * @return unlock result
     */
    public boolean unlock(String key) {
        log.info("Unlock key {}...", key);

        /*
           先移除的话如果解锁失败了会产生什么影响？
           1. 锁不是自己加的-也不该由自己解，移除不受影响
           2. 锁是自己加的-移除失败也不会再续期了
         */
        locks.remove(LOCK_PREFIX + key);
        boolean r = redisLock.unlock(key);
        if (!r) {
            log.error("Unlock {} end with result {}", key, r);
        }

        return r;
    }

    /**
     * 根据Key解锁资源
     *
     * @param key        key
     * @return 是否成功
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
     * 定时任务：刷新锁超时时间
     * locks记录的是锁的最大超时时间，如果加锁的任务因为各种原因迟迟无法解锁，超过最大超时时间后不会再刷新
     * 此为设计，有特殊需求可以更改
     * NOTE：任务的运行周期需要考虑设为多少更合适
     */
    @Scheduled(fixedDelay = 50_000)
    public void refreshLock() {
        Iterator<Map.Entry<String, LocalDateTime>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            // 锁已经过期了，移除记录
            if (entry.getValue().isBefore(LocalDateTime.now())) {
                iterator.remove();
            }

            // 锁没有过期，刷新锁的过期时间，但不会再更新记录的锁信息了
            redisLock.refreshLockExpire(entry.getKey(), Duration.ofSeconds(LOCK_TIMEOUT));
        }
    }

    /**
     * 刷新锁超时时间，增加了重试逻辑
     * locks记录的是锁的最大超时时间，如果加锁的任务因为各种原因迟迟无法解锁，超过最大超时时间后不会再刷新
     * 此为设计，有特殊需求可以更改
     */
    @Scheduled(fixedDelay = 50_000)
    public void refreshLockWithRetry() {
        Iterator<Map.Entry<String, LocalDateTime>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            // 锁已经过期了，移除记录
            if (entry.getValue().isBefore(LocalDateTime.now())) {
                iterator.remove();
                continue;
            }

            // 锁没有过期，刷新锁的过期时间
            int retryCount = 3;
            while (retryCount-- > 0) {
                try {
                    // 锁的续期：续期操作只是延长了锁的过期时间，但不会更新locks中的最大超时时间。
                    // 如果一个锁的续期次数超过了最大超时时间，锁依然会被释放。
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
     * 无限制刷新锁超时时间，任务未完成可以一直刷新
     * 当任务还在执行(锁未被删除)，任务会定期刷新锁的过期时间，并且更新本地缓存的所信息
     * 防止出现本地记录中锁过期了无法刷新，但任务仍在执行的情况
     */
    @Scheduled(fixedDelay = 50_000)
    public void refreshLockWithoutLimit() {
        Iterator<Map.Entry<String, LocalDateTime>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            // 锁已经过期了，移除记录
            if (entry.getValue().isBefore(LocalDateTime.now())) {
                iterator.remove();
                continue;
            }

            // 锁没有过期，刷新锁的过期时间
            int retryCount = 3;
            while (retryCount-- > 0) {
                try {
                    boolean refreshed = redisLock.refreshLockExpire(entry.getKey(), Duration.ofSeconds(LOCK_TIMEOUT));
                    if (refreshed) {
                        // NOTE:更新锁的最大超时时间
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
