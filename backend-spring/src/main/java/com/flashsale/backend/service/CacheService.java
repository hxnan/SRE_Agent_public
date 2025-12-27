package com.flashsale.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CacheService {
    private final StringRedisTemplate redis;

    public CacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void updateActiveByStock(long goodsId, long stock) {
        if (stock <= 0) {
            try { redis.opsForZSet().remove("goods:active_by_stock", String.valueOf(goodsId)); } catch (Exception ignored) {}
        } else {
            try { redis.opsForZSet().add("goods:active_by_stock", String.valueOf(goodsId), (double) stock); } catch (Exception ignored) {}
        }
    }

    public void invalidateProductListCaches() {
        try {
            Set<String> keys = redis.keys("products:*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ignored) {}
    }
}

