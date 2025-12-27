package com.flashsale.backend.kafka;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.OrderStatus;
import com.flashsale.backend.repository.SeckillOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.flashsale.backend.service.MetricsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderConsumer {
    private final SeckillOrderRepository orderRepo;
    private final StringRedisTemplate redis;
    private final LoggerService log;
    @Value("${kafka.order-topic}")
    private String topic;
    @Value("${kafka.consumer-group}")
    private String group;
    @Value("${kafka.order-dlq-topic}")
    private String dlqTopic;
    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong paidUpdates = new AtomicLong();
    private final AtomicLong skippedNotFound = new AtomicLong();
    private final AtomicLong skippedNotPending = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();

    private final KafkaTemplate<String, String> kafka;
    private final MetricsService metricsService;

    public OrderConsumer(SeckillOrderRepository orderRepo, StringRedisTemplate redis, LoggerService log, KafkaTemplate<String, String> kafka, MetricsService metricsService) {
        this.orderRepo = orderRepo;
        this.redis = redis;
        this.log = log;
        this.kafka = kafka;
        this.metricsService = metricsService;
    }

    @KafkaListener(topics = "${kafka.order-topic}", groupId = "${kafka.consumer-group}")
    public void handle(String message) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(message);
            String externalOrderId = node.path("order_id").asText();
            long goodsId = node.path("goods_id").asLong();
            double price = node.path("price").isNumber() ? node.path("price").asDouble() : 0.0;
            String requestId = node.path("request_id").asText(null);
            java.util.Map<String,Object> ctx1 = new java.util.HashMap<>();
            ctx1.put("requestId", String.valueOf(requestId));
            ctx1.put("order_id", externalOrderId);
            ctx1.put("goods_id", goodsId);
            ctx1.put("price", price);
            log.info("kafka_order_received", ctx1);
            String processedKey = "order_processed:" + externalOrderId;
            Boolean first = redis.opsForValue().setIfAbsent(processedKey, "1", java.time.Duration.ofHours(48));
            if (Boolean.FALSE.equals(first)) {
                java.util.Map<String,Object> ctx2 = new java.util.HashMap<>();
                ctx2.put("requestId", String.valueOf(requestId));
                ctx2.put("order_id", externalOrderId);
                log.info("order_exists_skip", ctx2);
            } else {
                try {
                    var existing = orderRepo.findByExternalOrderId(externalOrderId);
                    if (existing.isPresent()) {
                        java.util.Map<String,Object> ctx3 = new java.util.HashMap<>();
                        ctx3.put("requestId", String.valueOf(requestId));
                        ctx3.put("order_id", externalOrderId);
                        ctx3.put("db_id", existing.get().getId());
                        log.info("order_db_exists_skip", ctx3);
                    } else {
                        com.flashsale.backend.model.SeckillOrder order = new com.flashsale.backend.model.SeckillOrder();
                        order.setUserId(node.path("user_id").asLong());
                        order.setGoodsId(goodsId);
                        order.setStatus(OrderStatus.PAID.getCode());
                        order.setCreateTime(java.time.LocalDateTime.now());
                        order.setUpdatedAt(java.time.LocalDateTime.now());
                        order.setExternalOrderId(externalOrderId);
                        orderRepo.save(order);
                        metricsService.recordSale(goodsId, price);
                        paidUpdates.incrementAndGet();
                        java.util.Map<String,Object> ctx4 = new java.util.HashMap<>();
                        ctx4.put("requestId", String.valueOf(requestId));
                        ctx4.put("order_id", externalOrderId);
                        ctx4.put("db_id", order.getId());
                        ctx4.put("goods_id", goodsId);
                        ctx4.put("price", price);
                        log.info("order_insert_success", ctx4);
                    }
                } catch (Exception ex) {
                    errors.incrementAndGet();
                    java.util.Map<String,Object> err = new java.util.HashMap<>();
                    err.put("requestId", String.valueOf(requestId));
                    err.put("order_id", externalOrderId);
                    err.put("error", String.valueOf(ex.getMessage()));
                    log.error("order_insert_error", err);
                    try { kafka.send(dlqTopic, externalOrderId, message); } catch (Exception ignored) {}
                }
            }
            long p = processed.incrementAndGet();
            if (p % 50 == 0) {
                java.util.Map<String,Object> ctx5 = new java.util.HashMap<>();
                ctx5.put("topic", topic);
                ctx5.put("group", group);
                ctx5.put("processed", p);
                ctx5.put("paid_updates", paidUpdates.get());
                ctx5.put("skipped_not_found", skippedNotFound.get());
                ctx5.put("skipped_not_pending", skippedNotPending.get());
                ctx5.put("errors", errors.get());
                log.info("order_consumer_heartbeat", ctx5);
            }
        } catch (Exception e) {
            errors.incrementAndGet();
            java.util.Map<String,Object> err2 = new java.util.HashMap<>();
            err2.put("error", String.valueOf(e.getMessage()));
            log.error("kafka_consume_order_error", err2);
        }
    }
}
