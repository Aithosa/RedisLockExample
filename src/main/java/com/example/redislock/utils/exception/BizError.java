package com.example.redislock.utils.exception;

import com.example.redislock.api.base.Response;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * Business Error
 */
@Slf4j
public class BizError extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final String code;
    private transient Object[] args = null;

    /**
     * Constructs a new BizError with the specified error code.
     *
     * @param code the error code
     */
    public BizError(String code) {
        super("Error description by code");
        this.code = code;
    }

    /**
     * Constructs a new BizError with the specified error code and detail message.
     *
     * @param code    the error code
     * @param message the detail message
     */
    public BizError(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructs a new BizError with the specified error code, detail message, and cause.
     *
     * @param code    the error code
     * @param message the detail message
     * @param cause   the cause
     */
    public BizError(String code, String message, Throwable cause) {
        super(message, cause, true, false);
        this.code = code;
    }

    /**
     * Constructs a new BizError with the specified error code and cause.
     *
     * @param code  the error code
     * @param cause the cause
     */
    public BizError(String code, Throwable cause) {
        super(cause.toString(), cause, true, false);
        this.code = code;
    }

    /**
     * Re-throws the given throwable as a RuntimeException if it is already one, otherwise,
     * wraps it into a BizException and throws it.
     *
     * @param t the throwable to re-throw
     */
    public static void reThrow(Throwable t) {
        if (t instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new BizException(t);
    }

    /**
     * Checks if the error code matches the specified one.
     *
     * @param code the error code to check against
     * @return true if the codes match, false otherwise
     */
    public boolean isError(String code) {
        return Objects.equals(this.code, code);
    }

    /**
     * Sets the additional arguments for the error.
     *
     * @param args the additional arguments
     * @return this BizError instance
     */
    public BizError with(Object... args) {
        this.args = args;
        return this;
    }

    /**
     * Retrieves the additional arguments.
     *
     * @param args the additional arguments
     * @return the additional arguments
     */
    public Object[] getArgs(Object... args) {
        return args;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (msg != null) {
            try {
                return MessageFormat.format(msg, args);
            } catch (Exception t) {
                log.error(t.getMessage(), t);
            }
        }
        return msg;
    }

    /**
     * Returns the raw message without formatting.
     *
     * @return the raw error message
     */
    public String getRawMessage() {
        return super.getMessage();
    }

    /**
     * Converts the error into a Response object.
     *
     * @return the response representing the error
     */
    public Response toResponse() {
        return new Response(code, getMessage());
    }

    @Override
    public String toString() {
        return "BizError:" + "(" + code + ")" + getMessage();
    }
}
