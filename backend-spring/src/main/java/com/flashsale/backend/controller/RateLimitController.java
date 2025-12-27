package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.security.RateLimitConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor/ratelimit")
public class RateLimitController {
    private final RateLimitConfigService config;
    private final LoggerService log;

    public RateLimitController(RateLimitConfigService config, LoggerService log) {
        this.config = config;
        this.log = log;
    }

    @GetMapping
    public ResponseEntity<?> get() {
        return ResponseEntity.ok(Map.of(
                "seckill_window_seconds", config.getSeckillWindowSeconds(),
                "seckill_max", config.getSeckillMax(),
                "api_window_seconds", config.getApiWindowSeconds(),
                "api_max", config.getApiMax()
        ));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody Map<String, Object> body) {
        Integer sWin = body.get("seckill_window_seconds") instanceof Number n ? n.intValue() : null;
        Integer sMax = body.get("seckill_max") instanceof Number n ? n.intValue() : null;
        Integer aWin = body.get("api_window_seconds") instanceof Number n ? n.intValue() : null;
        Integer aMax = body.get("api_max") instanceof Number n ? n.intValue() : null;
        config.update(sWin, sMax, aWin, aMax);
        log.info("ratelimit_updated", Map.of(
                "seckill_window_seconds", config.getSeckillWindowSeconds(),
                "seckill_max", config.getSeckillMax(),
                "api_window_seconds", config.getApiWindowSeconds(),
                "api_max", config.getApiMax()
        ));
        return get();
    }
}

