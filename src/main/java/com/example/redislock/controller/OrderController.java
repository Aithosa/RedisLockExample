package com.example.redislock.controller;

import com.example.redislock.api.base.Response;
import com.example.redislock.api.order.SubmitOrderReq;
import com.example.redislock.aspect.paramter.RedisLockCheck;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;

@RequestMapping(path = "/order")
public class OrderController {
    @PostMapping(value = "/book/submit")
    public Response submitOrder(@Valid @RequestBody @RedisLockCheck(timeout = 120 * 1000) SubmitOrderReq req) {
        return Response.success();
    }
}
