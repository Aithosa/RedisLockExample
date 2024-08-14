package com.example.redislock.utils.errorinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Notes annotation for adding remarks to fields.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Notes {
    /**
     * The remark or note for the annotated field.
     *
     * @return the note
     */
    String value();
}