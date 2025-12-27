package com.flashsale.backend.service;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.SeckillGoods;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OrderEventServiceTest {
    @Test
    void outOfStock_returnsFail() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        KafkaTemplate<String, String> kafka = Mockito.mock(KafkaTemplate.class);
        LoggerService log = Mockito.mock(LoggerService.class);
        CacheService cacheService = new CacheService(redis);
        OrderEventService svc = new OrderEventService(redis, kafka, log, "order-events", cacheService);
        SeckillGoods goods = new SeckillGoods();
        goods.setId(1L);
        goods.setStock(0);
        goods.setSeckillPrice(BigDecimal.ONE);
        goods.setStartTime(LocalDateTime.now().minusMinutes(1));
        goods.setEndTime(LocalDateTime.now().plusMinutes(1));
        var valueOps = Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("0");
        when(redis.execute(any(), anyList())).thenReturn(-1L);
        Map<String, Object> res = svc.processSeckill(10L, goods, "req1");
        assertEquals(Boolean.FALSE, res.get("success"));
        verify(kafka, times(0)).send(org.mockito.ArgumentMatchers.<org.apache.kafka.clients.producer.ProducerRecord<String, String>>any());
    }

    @Test
    void inStock_sendsKafka_and_success() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        KafkaTemplate<String, String> kafka = Mockito.mock(KafkaTemplate.class);
        LoggerService log = Mockito.mock(LoggerService.class);
        CacheService cacheService = new CacheService(redis);
        OrderEventService svc = new OrderEventService(redis, kafka, log, "order-events", cacheService);
        SeckillGoods goods = new SeckillGoods();
        goods.setId(2L);
        goods.setStock(10);
        goods.setSeckillPrice(BigDecimal.TEN);
        goods.setStartTime(LocalDateTime.now().minusMinutes(1));
        goods.setEndTime(LocalDateTime.now().plusMinutes(1));
        var valueOps2 = Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps2);
        when(valueOps2.get(anyString())).thenReturn("10");
        when(redis.execute(any(), anyList())).thenReturn(9L);
        Map<String, Object> res = svc.processSeckill(20L, goods, "req2");
        assertEquals(Boolean.TRUE, res.get("success"));
        assertNotNull(res.get("order_id"));
        verify(kafka, times(1)).send(org.mockito.ArgumentMatchers.<org.apache.kafka.clients.producer.ProducerRecord<String, String>>any());
    }
}
