package com.example.redislock.api.base;

import com.example.redislock.utils.errorinfo.ErrorCodes;
import com.example.redislock.utils.exception.BizError;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.text.MessageFormat;

/**
 * Response<T> class represents a standardized response format for API responses.
 *
 * @param <T> the type of the data included in the response
 */
@ApiModel(description = "Response message")
@Data
@Slf4j
public class Response<T> {
    /**
     * Result code, where 0 indicates successful operation.
     */
    @ApiModelProperty(value = "Result code, 0 indicates success", required = true, example = "0")
    protected String resultCode = "0";

    /**
     * Description of the result.
     */
    @ApiModelProperty(value = "Description of the result", required = true, example = "Operation successful")
    protected String description = "Operation successful";

    /**
     * Response data.
     */
    @ApiModelProperty(value = "Response data", notes = "Can be ignored if there is no specific type")
    protected T data;

    /**
     * Error parameters.
     */
    @ApiModelProperty(value = "Error parameters", hidden = true)
    @JsonIgnore
    protected Object[] args;

    /**
     * Default constructor.
     */
    public Response() {
    }

    /**
     * Constructor with response data.
     *
     * @param data the response data
     */
    public Response(T data) {
        this.data = data;
    }

    /**
     * Constructor with resultCode and description.
     *
     * @param resultCode the result code
     * @param msg        the description
     */
    @JsonCreator
    public Response(@JsonProperty("resultCode") String resultCode, @JsonProperty("description") String msg) {
        this.resultCode = resultCode;
        this.description = msg;
    }

    /**
     * Constructor with resultCode, description, and data.
     *
     * @param resultCode the result code
     * @param desc       the description
     * @param data       the response data
     */
    public Response(String resultCode, String desc, T data) {
        this.resultCode = resultCode;
        this.description = desc;
        this.data = data;
    }

    /**
     * Binds additional parameters to the error description.
     *
     * @param args additional parameters
     * @return the instance of Response<T>
     */
    public Response<T> withArgs(Object... args) {
        this.args = args;
        if (description != null && args != null && args.length > 0) {
            try {
                description = MessageFormat.format(description, args);
            } catch (Exception t) {
                log.error(t.getMessage(), t);
            }
        }
        return this;
    }

    /**
     * Creates a successful response.
     *
     * @param data the response data
     * @param <T>  the type of the response data
     * @return a successful Response instance
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(data);
    }

    /**
     * Creates a successful response.
     *
     * @param <T> the type of the response data
     * @return a successful Response instance
     */
    public static <T> Response<T> success() {
        return new Response<>();
    }

    /**
     * Creates a failed response.
     *
     * @param code the error code
     * @param <T>  the type of the response data
     * @return a failed Response instance
     */
    public static <T> Response<T> fail(String code) {
        return fail(code, ErrorCodes.getErrorDesc(code));
    }

    /**
     * Creates a failed response with data.
     *
     * @param code the error code
     * @param data the response data
     * @param <T>  the type of the response data
     * @return a failed Response instance
     */
    public static <T> Response<T> fail(String code, T data) {
        return fail(code, ErrorCodes.getErrorDesc(code), data);
    }

    /**
     * Creates a failed response.
     *
     * @param code the error code
     * @param msg  the error message
     * @param <T>  the type of the response data
     * @return a failed Response instance
     */
    public static <T> Response<T> fail(String code, String msg) {
        return new Response<>(code, msg);
    }

    /**
     * Creates a failed response with data.
     *
     * @param code the error code
     * @param msg  the error message
     * @param data the response data
     * @param <T>  the type of the response data
     * @return a failed Response instance
     */
    public static <T> Response<T> fail(String code, String msg, T data) {
        return new Response<>(code, msg, data);
    }

    /**
     * Checks if the response is successful.
     *
     * @return true if the resultCode indicates success, false otherwise
     */
    public boolean isSuccess() {
        return ErrorCodes.SUCCESS.equals(resultCode);
    }

    /**
     * Checks if the response is successful, throws an exception if it is not.
     *
     * @param action  the action description for logging
     * @param rt      the response to check
     * @param optCode the error code to use if the response is not successful
     */
    public static void check(String action, Response<?> rt, String optCode) {
        check(action, rt, optCode, ErrorCodes.getErrorDesc(optCode));
    }

    /**
     * Checks the response, throws an exception if it is not successful.
     *
     * @param action  the action description for logging
     * @param rt      the response to check
     * @param optCode the error code to use if the response is not successful
     * @param optDes  the error description to use if the response is not successful
     */
    public static void check(String action, Response<?> rt, String optCode, String optDes) {
        if (rt == null) {
            log.error("{} response is null", action);
            throw new BizError(optCode, optDes);
        }

        if (!ErrorCodes.SUCCESS.equals(rt.getResultCode())) {
            if (!StringUtils.isEmpty(rt.getDescription())) {
                optDes = rt.getDescription();
            }
            log.error("{} response fail {}", action, rt);
            throw new BizError(rt.getResultCode(), optDes);
        }
    }
}
