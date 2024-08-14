package com.example.redislock.aspect.any;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for methods that should be executed with a Redis lock.
 *
 * <p>This annotation can be applied to methods to ensure that they are executed
 * with a distributed Redis lock. The lock is identified by a key and has a
 * configurable timeout and time unit.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    /**
     * The key used for the Redis lock.
     *
     * @return the key as a {@code String}
     */
    String key();

    /**
     * The timeout for the lock in the specified {@code timeUnit}.
     * Default value is 600 seconds.
     *
     * @return the timeout as an {@code int}
     */
    int timeout() default 600; // Default timeout is 600 seconds

    /**
     * The time unit for the timeout.
     * Default value is {@code TimeUnit.SECONDS}.
     *
     * @return the time unit as a {@code TimeUnit}
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS; // Default time unit is seconds
}