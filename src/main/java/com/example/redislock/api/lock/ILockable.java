package com.example.redislock.api.lock;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 加锁接口
 * 具体锁的key是什么需要在请求里实现getLockKey方法，在其中自己定义
 */
public interface ILockable {
    @JsonIgnore
    String getLockKey();
}
