package com.example.redislock.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisScriptingCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Implement distributed locking using Redis
 */
@Slf4j
public class RedisLock {
    /**
     * Unlock script
     */
    private static final String UNLOCK_LUA = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then return redis.call(\"del\", KEYS[1]) else return 0 end";

    /**
     * Update expiration time
     */
    private static final String EXPIRE_LUA = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then return redis.call(\"expire\", KEYS[1], ARGV[2]) else return 0 end";

    /**
     * Current node
     */
    private final String nodeId;

    private final StringRedisTemplate strRedis;

    public RedisLock(String nodeId, StringRedisTemplate strRedis) {
        this.nodeId = nodeId;
        this.strRedis = strRedis;
    }

    /**
     * Acquire lock
     * <p> Redis storage:
     * <p>  - Key is a custom value passed in by the request parameters
     * <p>  - Value is the current node Id (randomly generated when creating LockService)
     *
     * @param key    The key
     * @param expire Expiration time
     * @return Whether the lock is acquired
     */
    public boolean lock(String key, Duration expire) {
        try {
            Boolean result = strRedis.execute((RedisCallback<Boolean>) connection -> {
                RedisStringCommands commands = connection.stringCommands();
                return commands.set(key.getBytes(StandardCharsets.UTF_8),
                        nodeId.getBytes(StandardCharsets.UTF_8),
                        Expiration.from(expire),
                        RedisStringCommands.SetOption.SET_IF_ABSENT);
            });
            return result == null ? false : result;
        } catch (Exception e) {
            log.error("Exception occurred while setting redis.", e);
        }

        return false;
    }

    /**
     * Release lock
     * <p> When releasing the lock, check if the Value corresponding to the lock Key is the node Id passed in during RedisLock initialization (randomly generated when creating LockService).
     * <p> Ensure that only the lock holder can release the lock.
     *
     * @param key The key
     * @return Whether the lock was released successfully
     */
    public boolean unlock(String key) {
        try {
            Long result = strRedis.execute((RedisCallback<Long>) connection -> {
                RedisScriptingCommands commands = connection.scriptingCommands();
                return commands.eval(UNLOCK_LUA.getBytes(StandardCharsets.UTF_8),
                        ReturnType.INTEGER, 1,
                        key.getBytes(StandardCharsets.UTF_8),
                        nodeId.getBytes(StandardCharsets.UTF_8));
            });
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Exception occurred while releasing lock.", e);
        }

        return false;
    }

    /**
     * Refresh lock expiration time
     *
     * @param key    The key
     * @param expire New expiration time
     * @return Whether the expiration time was refreshed successfully
     */
    public boolean refreshLockExpire(String key, Duration expire) {
        try {
            Long result = strRedis.execute((RedisCallback<Long>) connection -> {
                RedisScriptingCommands commands = connection.scriptingCommands();
                return commands.eval(EXPIRE_LUA.getBytes(StandardCharsets.UTF_8),
                        ReturnType.INTEGER, 1,
                        key.getBytes(StandardCharsets.UTF_8),
                        nodeId.getBytes(StandardCharsets.UTF_8),
                        String.valueOf(expire.toMillis() / 1000).getBytes(StandardCharsets.UTF_8));
            });

            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Exception occurred while refreshing lock expiration.", e);
        }

        return false;
    }
}
