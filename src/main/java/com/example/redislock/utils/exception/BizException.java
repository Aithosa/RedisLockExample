package com.example.redislock.utils.exception;

import java.io.Serial;

/**
 * Custom business exception class.
 */
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public BizException() {
    }

    /**
     * Constructor with exception message.
     *
     * @param message Exception message.
     */
    public BizException(String message) {
        super(message);
    }

    /**
     * Constructor with exception cause.
     *
     * @param cause Exception cause.
     */
    public BizException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with exception message and cause.
     *
     * @param message Exception message.
     * @param cause   Exception cause.
     */
    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with exception message, cause, suppression flag and writable stack trace flag.
     *
     * @param message            Exception message.
     * @param cause              Exception cause.
     * @param enableSuppression  Flag to enable suppression.
     * @param writableStacktrace Flag to enable writable stack trace.
     */
    public BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStacktrace) {
        super(message, cause, enableSuppression, writableStacktrace);
    }
}
