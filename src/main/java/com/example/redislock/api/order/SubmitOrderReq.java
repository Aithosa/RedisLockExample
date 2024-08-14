package com.example.redislock.api.order;

import com.example.redislock.api.lock.ILockable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * This class represents the request to submit an order.
 */
public class SubmitOrderReq implements ILockable {

    /**
     * The ID of the order to be modified.
     */
    @ApiModelProperty("The ID of the order to be modified.")
    @NotBlank
    private String orderId;

    /**
     * Remark for the order.
     */
    @ApiModelProperty("Remark for the order.")
    private String remark;

    /**
     * Gets the lock key which is the order ID.
     *
     * @return the order ID as the lock key.
     */
    @Override
    @JsonIgnore
    public String getLockKey() {
        return orderId;
    }
}