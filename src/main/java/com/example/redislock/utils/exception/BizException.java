package com.example.redislock.utils.exception;

import java.io.Serial;

public class BizException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造
     */
    public BizException() {
    }

    /**
     * 构造
     *
     * @param message 异常消息
     */
    public BizException(String message) {
        super(message);
    }

    /**
     * 构造
     *
     * @param cause 异常原因
     */
    public BizException(Throwable cause) {
        super(cause);
    }

    /**
     * 构造
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造
     *
     * @param message            异常消息
     * @param cause              异常原因
     * @param enableSuppression  是否抑制
     * @param writableStacktrace 是否写stackTrace
     */
    public BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStacktrace) {
        super(message, cause, enableSuppression, writableStacktrace);
    }
}
