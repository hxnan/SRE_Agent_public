package com.flashsale.backend.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitConfigService {
    private final StringRedisTemplate redis;

    private volatile int seckillWindowSeconds = 10;
    private volatile int seckillMax = 3;
    private volatile int apiWindowSeconds = 60;
    private volatile int apiMax = 100;

    public RateLimitConfigService(StringRedisTemplate redis) {
        this.redis = redis;
        loadFromRedis();
    }

    public synchronized void update(Integer seckillWindowSeconds, Integer seckillMax, Integer apiWindowSeconds, Integer apiMax) {
        if (seckillWindowSeconds != null) this.seckillWindowSeconds = seckillWindowSeconds;
        if (seckillMax != null) this.seckillMax = seckillMax;
        if (apiWindowSeconds != null) this.apiWindowSeconds = apiWindowSeconds;
        if (apiMax != null) this.apiMax = apiMax;
        persistToRedis();
    }

    public synchronized void loadFromRedis() {
        try {
            String sWin = redis.opsForValue().get("ratelimit:seckill:window_seconds");
            String sMax = redis.opsForValue().get("ratelimit:seckill:max");
            String aWin = redis.opsForValue().get("ratelimit:api:window_seconds");
            String aMax = redis.opsForValue().get("ratelimit:api:max");
            if (sWin != null) this.seckillWindowSeconds = Integer.parseInt(sWin);
            if (sMax != null) this.seckillMax = Integer.parseInt(sMax);
            if (aWin != null) this.apiWindowSeconds = Integer.parseInt(aWin);
            if (aMax != null) this.apiMax = Integer.parseInt(aMax);
        } catch (Exception ignored) {}
    }

    private void persistToRedis() {
        try {
            redis.opsForValue().set("ratelimit:seckill:window_seconds", String.valueOf(seckillWindowSeconds));
            redis.opsForValue().set("ratelimit:seckill:max", String.valueOf(seckillMax));
            redis.opsForValue().set("ratelimit:api:window_seconds", String.valueOf(apiWindowSeconds));
            redis.opsForValue().set("ratelimit:api:max", String.valueOf(apiMax));
        } catch (Exception ignored) {}
    }

    public int getSeckillWindowSeconds() { return seckillWindowSeconds; }
    public int getSeckillMax() { return seckillMax; }
    public int getApiWindowSeconds() { return apiWindowSeconds; }
    public int getApiMax() { return apiMax; }
}

