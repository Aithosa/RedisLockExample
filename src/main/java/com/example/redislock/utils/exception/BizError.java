package com.example.redislock.utils.exception;

import com.example.redislock.api.base.Response;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * 业务错误
 */
@Slf4j
public class BizError extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final String code;

    private transient Object[] args = null;

    public BizError(String code) {
        super("Error description by code");
        this.code = code;
    }

    public BizError(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizError(String code, String message, Throwable cause) {
        super(message, cause, true, false);
        this.code = code;
    }

    public BizError(String code, Throwable cause) {
        super(cause.toString(), cause, true, false);
        this.code = code;
    }

    /**
     * 重新抛出
     *
     * @param t 异常
     */
    public static void reThrow(Throwable t) {
        if (t instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new BizException(t);
    }

    /**
     * 是否某种错误
     *
     * @param code 错误码
     * @return 是否相等
     */
    public boolean isError(String code) {
        return Objects.equals(this.code, code);
    }

    /**
     * 错误参数
     *
     * @param args 参数
     * @return 本身
     */
    public BizError with(Object... args) {
        this.args = args;
        return this;
    }

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
     * 原始消息
     *
     * @return 错误码
     */
    public String getRawMessage() {
        return super.getMessage();
    }

    /**
     * 返回响应
     *
     * @return 响应
     */
    public Response toResponse() {
        return new Response(code, getMessage());
    }

    @Override
    public String toString() {
        return "BizError:" + "(" + code + ")" + getMessage();
    }
}
