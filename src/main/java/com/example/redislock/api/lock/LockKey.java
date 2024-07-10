package com.example.redislock.api.lock;

import lombok.Data;

/**
 * LockKey
 */
@Data
public class LockKey {
    /**
     * 锁的key
     */
    private String key;

    /**
     * 锁的超时时间，单位毫秒
     */
    private int timeout;
}