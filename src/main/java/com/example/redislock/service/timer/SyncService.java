package com.example.redislock.service.timer;

import com.example.redislock.aspect.any.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SyncService {
    @RedisLock(key = "sync-order")
    @Async
    public void sync() {
        // 处理同步逻辑
        log.info("Sync process started");
        // 模拟长时间任务
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sync process interrupted", e);
        }
        log.info("Sync process completed");
    }
}
