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
 * 错误码信息
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ErrorCodes {
    @Notes("操作成功")
    public static final String SUCCESS = "0";

    @Notes("未知错误")
    public static final String FAIL = "-1";

    private static final Map<String, String> ERRORS = new HashMap<>();

    /**
     * 设置错误描述
     *
     * @param code  错误码
     * @param value 描述
     * @return 旧的描述
     */
    public static synchronized String setErrorDesc(String code, String value) {
        return ERRORS.put(code, value);
    }

    /**
     * 获取错误描述
     *
     * @param code 错误码
     * @return 描述
     */
    public static String getErrorDesc(String code) {
        return ERRORS.get(code);
    }

    /**
     * 获取错误描述
     *
     * @param code 错误码
     * @param def  默认描述
     * @return 描述
     */
    public static String getErrorDesc(String code, String def) {
        return ERRORS.getOrDefault(code, def);
    }

    /**
     * 初始化
     *
     * @param clazz  错误码继承类
     * @param prefix 错误码前缀
     */
    public static void initDesc(Class<?> clazz, String prefix) {
        // 处理应用类定义的常量，不允许继承关系，必须是public static final
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
            log.error("load error description fail", e);
        }
    }

    private static Optional<String> getFieldDesc(Field fd) {
        int mf = fd.getModifiers();
        if (Modifier.isPublic(mf) && Modifier.isStatic(mf) && Modifier.isFinal(mf) && fd.getType() == String.class) {
            Notes nt = fd.getAnnotation(Notes.class);
            return nt == null || StringUtils.isEmpty(nt.value()) ? Optional.empty() : Optional.of(nt.value());
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
            log.error("load error code fail", e);
        }
    }
}
