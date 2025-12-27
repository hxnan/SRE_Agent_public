package com.flashsale.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final StringRedisTemplate redis;

    public MetricsService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void recordSale(long goodsId, double price) {
        var turnoverKey = "mall:turnover";
        var totalSoldKey = "mall:sold_count";
        var goodsSoldKey = "goods:sold_count:" + goodsId;
        redis.opsForValue().increment(totalSoldKey);
        redis.opsForValue().increment(goodsSoldKey);
        redis.opsForValue().increment(turnoverKey, price);
    }
}

