package com.example.redislock.controller;

import com.example.redislock.api.base.Response;
import com.example.redislock.service.timer.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/timer")
public class TimerController {
    @Autowired
    private SyncService syncService;

    @PostMapping(value = "/sync/order")
    public Response syncOrder() {
        syncService.sync();
        return Response.success();
    }
}
