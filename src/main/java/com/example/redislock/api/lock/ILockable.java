package com.example.redislock.api.lock;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Lockable Interface
 * <p> You need to implement the `getLockKey` method in the request
 * to define what the specific lock key is.
 */
public interface ILockable {

    /**
     * This method should be implemented to provide the lock key.
     *
     * @return the lock key as a String
     */
    @JsonIgnore
    String getLockKey();
}
