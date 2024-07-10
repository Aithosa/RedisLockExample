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

@ApiModel(description = "响应消息")
@Data
@Slf4j
public class Response<T> {
    @ApiModelProperty(value = "错误码，0表示操作成功", required = true, example = "0")
    protected String resultCode = "0";

    @ApiModelProperty(value = "错误描述", required = true, example = "操作成功")
    protected String description = "操作成功";

    @ApiModelProperty(value = "响应数据", notes = "没有具体类型的可以忽略")
    protected T data;

    @ApiModelProperty(value = "错误参数", hidden = true)
    @JsonIgnore
    protected Object[] args;

    public Response() {
    }

    public Response(T data) {
        this.data = data;
    }

    @JsonCreator
    public Response(@JsonProperty("resultCode") String resultCode, @JsonProperty("description") String msg) {
        this.resultCode = resultCode;
        this.description = msg;
    }

    public Response(String resultCode, String desc, T data) {
        this.resultCode = resultCode;
        this.description = desc;
        this.data = data;
    }

    /**
     * 绑定参数
     *
     * @param args 参数
     * @return 自身
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
     * 构造成功响应
     *
     * @param data 内容
     * @param <T>  类型
     * @return 响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(data);
    }

    /**
     * 构造成功响应
     *
     * @param <T> 类型
     * @return 响应
     */
    public static <T> Response<T> success() {
        return new Response<>();
    }

    /**
     * 失败响应
     *
     * @param code 错误码
     * @param <T>  类型
     * @return 响应
     */
    public static <T> Response<T> fail(String code) {
        return fail(code, ErrorCodes.getErrorDesc(code));
    }

    /**
     * 失败响应，带数据
     *
     * @param code 错误码
     * @param data 类型
     * @param <T>  错误码描述
     * @return 响应
     */
    public static <T> Response<T> fail(String code, T data) {
        return fail(code, ErrorCodes.getErrorDesc(code), data);
    }

    /**
     * 失败响应，带数据
     *
     * @param code 错误码
     * @param <T>  错误码描述
     * @return 响应
     */
    public static <T> Response<T> fail(String code, String msg) {
        return new Response<>(code, msg);
    }

    /**
     * 失败响应，带数据
     *
     * @param code 错误码
     * @param msg  错误码描述
     * @param data 类型
     * @param <T>  错误码描述
     * @return 响应
     */
    public static <T> Response<T> fail(String code, String msg, T data) {
        return new Response<>(code, msg, data);
    }

    /**
     * 判断是否成功
     *
     * @return 标识成功
     */
    public boolean isSuccess() {
        return ErrorCodes.SUCCESS.equals(resultCode);
    }

    /**
     * 检查是否成功，失败则抛出异常
     *
     * @param action  日志记录内容
     * @param rt      响应消息
     * @param optCode 异常错误吗
     */
    public static void check(String action, Response<?> rt, String optCode) {
        check(action, rt, optCode, ErrorCodes.getErrorDesc(optCode));
    }

    /**
     * 检查响应， 失败抛出异常
     *
     * @param action  日志记录内容
     * @param rt      响应消息
     * @param optCode 异常错误吗
     * @param optDes  错误描述
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
