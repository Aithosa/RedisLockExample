package com.example.redislock.api.order;

import com.example.redislock.api.lock.ILockable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

public class SubmitOrderReq implements ILockable {
    @ApiModelProperty("修改订单Id")
    @NotBlank
    private String orderId;

    @ApiModelProperty("订单备注")
    private String remark;

    @Override
    @JsonIgnore
    public String getLockKey() {
        return orderId;
    }
}
