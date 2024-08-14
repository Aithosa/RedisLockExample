package com.example.redislock.api.lock;

import lombok.Data;

/**
 * Represents a lock key and its timeout duration.
 */
@Data
public class LockKey {
    /**
     * The key for the lock.
     * <p>
     * This field represents the unique key that identifies the lock.
     * </p>
     */
    private String key;

    /**
     * The timeout duration for the lock in milliseconds.
     * <p>
     * This field represents the timeout duration of the lock in milliseconds.
     * The lock will be automatically released after this duration.
     * </p>
     */
    private int timeout;
}
