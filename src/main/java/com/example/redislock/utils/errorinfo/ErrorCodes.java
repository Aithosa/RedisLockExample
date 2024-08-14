package com.example.redislock.utils.errorinfo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Error Code Information
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ErrorCodes {

    @Notes("Operation Successful")
    public static final String SUCCESS = "0";

    @Notes("Unknown Error")
    public static final String FAIL = "-1";

    private static final Map<String, String> ERRORS = new HashMap<>();

    /**
     * Sets the error description for a given error code.
     *
     * @param code  Error code
     * @param value Description
     * @return The old description
     */
    public static synchronized String setErrorDesc(String code, String value) {
        return ERRORS.put(code, value);
    }

    /**
     * Retrieves the error description for a given error code.
     *
     * @param code Error code
     * @return Description
     */
    public static String getErrorDesc(String code) {
        return ERRORS.get(code);
    }

    /**
     * Retrieves the error description for a given error code, or returns
     * a default description if the code is not found.
     *
     * @param code Error code
     * @param def  Default description
     * @return Description
     */
    public static String getErrorDesc(String code, String def) {
        return ERRORS.getOrDefault(code, def);
    }

    /**
     * Initializes error descriptions.
     *
     * @param clazz  The class containing error codes
     * @param prefix The prefix to use for error codes
     */
    public static void initDesc(Class<?> clazz, String prefix) {
        // Process constants defined in the application class: must be public static final
        for (Field fd : clazz.getDeclaredFields()) {
            Optional<String> desc = getFieldDesc(fd);
            desc.ifPresent(s -> resolveDesc(fd, s));
        }
        updatePrefix(prefix);
    }

    private static void resolveDesc(Field fd, String desc) {
        try {
            Object ov = fd.get(null);
            if (ov instanceof String code) {
                ERRORS.put(code, desc);
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to load error description", e);
        }
    }

    private static Optional<String> getFieldDesc(Field fd) {
        int mf = fd.getModifiers();
        if (Modifier.isPublic(mf) && Modifier.isStatic(mf) &&
                Modifier.isFinal(mf) && fd.getType() == String.class) {
            Notes nt = fd.getAnnotation(Notes.class);
            return nt == null || StringUtils.isEmpty(nt.value()) ?
                    Optional.empty() : Optional.of(nt.value());
        }
        return Optional.empty();
    }

    private static void updatePrefix(String prefix) {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            for (Field fd : ErrorCodes.class.getFields()) {
                Object ov = fd.get(null);
                if (!(ov instanceof String code)) {
                    continue;
                }
                Notes n = fd.getAnnotation(Notes.class);
                String desc = n == null ? null : n.value();
                if (code.length() > 2) {
                    fd.setAccessible(true);
                    modifiersField.setInt(fd, fd.getModifiers() & ~Modifier.FINAL);
                    fd.set(null, prefix + code);
                    ERRORS.put(prefix + code, desc);
                } else {
                    ERRORS.put(code, desc);
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("Failed to load error codes", e);
        }
    }
}