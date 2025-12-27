package com.flashsale.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.SeckillGoods;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Service
public class OrderEventService {
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafka;
    private final LoggerService log;
    private final String orderTopic;
    private final CacheService cacheService;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderEventService(StringRedisTemplate redis,
                             KafkaTemplate<String, String> kafka,
                             LoggerService log,
                             @Value("${kafka.order-topic}") String orderTopic,
                             CacheService cacheService) {
        this.redis = redis;
        this.kafka = kafka;
        this.log = log;
        this.orderTopic = orderTopic;
        this.cacheService = cacheService;
    }

    public Map<String, Object> processSeckill(Long userId, SeckillGoods product, Object requestIdAttr) {
        var now = java.time.LocalDateTime.now();
        long goodsId = product.getId();
        String stockKey = "seckill:stock:" + goodsId;
        String currentStock = redis.opsForValue().get(stockKey);
        java.util.Map<String,Object> ctx1 = new java.util.HashMap<>();
        ctx1.put("requestId", String.valueOf(requestIdAttr));
        ctx1.put("userId", userId);
        ctx1.put("goods_id", goodsId);
        ctx1.put("cacheHit", currentStock != null);
        log.info("seckill_check_stock", ctx1);
        if (currentStock == null) {
            currentStock = String.valueOf(product.getStock());
            redis.opsForValue().set(stockKey, currentStock);
            java.util.Map<String,Object> ctx2 = new java.util.HashMap<>();
            ctx2.put("requestId", String.valueOf(requestIdAttr));
            ctx2.put("goods_id", goodsId);
            ctx2.put("stock", product.getStock());
            log.info("seckill_init_stock_cache", ctx2);
            if ((product.getStartTime().isBefore(now) || product.getStartTime().isEqual(now)) && product.getEndTime().isAfter(now)) {
                try { redis.opsForZSet().add("goods:active_by_stock", String.valueOf(goodsId), product.getStock()); } catch (Exception ignored) {}
            }
        }
        String script = "local v=redis.call('GET', KEYS[1]); if not v then return -2 end; local n=tonumber(v); if n and n>0 then local r=redis.call('DECR', KEYS[1]); return r else return -1 end";
        var lua = new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(script, Long.class);
        Long newStock = redis.execute(lua, java.util.Collections.singletonList(stockKey));
        java.util.Map<String,Object> ctx3 = new java.util.HashMap<>();
        ctx3.put("requestId", String.valueOf(requestIdAttr));
        ctx3.put("goods_id", goodsId);
        ctx3.put("newStock", newStock);
        log.info("seckill_decrement_stock", ctx3);
        if (newStock == null || newStock < 0) {
            cacheService.updateActiveByStock(goodsId, 0);
            return Map.of("success", false, "message", "Product out of stock");
        }
        try {
            String externalOrderId = java.util.UUID.randomUUID().toString();
            java.util.Map<String,Object> ctx4 = new java.util.HashMap<>();
            ctx4.put("requestId", String.valueOf(requestIdAttr));
            ctx4.put("userId", userId);
            ctx4.put("goods_id", goodsId);
            ctx4.put("orderId", externalOrderId);
            log.info("seckill_order_event_prepared", ctx4);
            Map<String, Object> payload = Map.of(
                    "event_type", "order_created",
                    "request_id", String.valueOf(requestIdAttr),
                    "order_id", externalOrderId,
                    "user_id", userId,
                    "goods_id", goodsId,
                    "price", product.getSeckillPrice() != null ? product.getSeckillPrice() : BigDecimal.ZERO,
                    "status", "PAID",
                    "created_at", System.currentTimeMillis()
            );
            String json = mapper.writeValueAsString(payload);
            ProducerRecord<String, String> record = new ProducerRecord<>(orderTopic, externalOrderId, json);
            record.headers().add("x-request-id", String.valueOf(requestIdAttr).getBytes());
            kafka.send(record);
            java.util.Map<String,Object> ctx5 = new java.util.HashMap<>();
            ctx5.put("topic", orderTopic);
            ctx5.put("payload", payload);
            log.info("kafka_produce_order_message", ctx5);
            try { cacheService.updateActiveByStock(goodsId, newStock); cacheService.invalidateProductListCaches(); } catch (Exception ignored) {}
            return Map.of("success", true, "order_id", externalOrderId, "message", "Seckill successful! Event published.");
        } catch (Exception e) {
            try { redis.opsForValue().increment(stockKey); } catch (Exception ignored) {}
            java.util.Map<String,Object> err = new java.util.HashMap<>();
            err.put("requestId", String.valueOf(requestIdAttr));
            err.put("error", String.valueOf(e.getMessage()));
            log.error("seckill_order_publish_error", err);
            try { cacheService.updateActiveByStock(goodsId, Math.max(0L, newStock != null ? newStock : 0L)); cacheService.invalidateProductListCaches(); } catch (Exception ignored) {}
            return Map.of("success", false, "message", "Internal server error");
        }
    }
}
