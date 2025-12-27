package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.OrderStatus;
import com.flashsale.backend.model.SeckillGoods;
import com.flashsale.backend.model.SeckillOrder;
import com.flashsale.backend.repository.SeckillGoodsRepository;
import com.flashsale.backend.repository.SeckillOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillGoodsRepository goodsRepo;
    private final SeckillOrderRepository orderRepo;
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafka;
    private final LoggerService log;
    private final String orderTopic;
    private final com.flashsale.backend.service.OrderEventService orderEventService;

    public SeckillController(SeckillGoodsRepository goodsRepo,
                             SeckillOrderRepository orderRepo,
                             StringRedisTemplate redis,
                             KafkaTemplate<String, String> kafka,
                             LoggerService log,
                             @Value("${kafka.order-topic}") String orderTopic,
                             com.flashsale.backend.service.OrderEventService orderEventService) {
        this.goodsRepo = goodsRepo;
        this.orderRepo = orderRepo;
        this.redis = redis;
        this.kafka = kafka;
        this.log = log;
        this.orderTopic = orderTopic;
        this.orderEventService = orderEventService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> seckill(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Object auth = req.getUserPrincipal();
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        Long userId = null;
        if (auth instanceof org.springframework.security.core.Authentication a) {
            Object p = a.getPrincipal();
            if (p instanceof java.util.Map<?, ?> m) {
                userId = ((Number) m.get("userId")).longValue();
            }
        }
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        Number goodsIdNum = (Number) body.get("goods_id");
        if (goodsIdNum == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Goods ID is required"));
        }
        long goodsId = goodsIdNum.longValue();
        Optional<SeckillGoods> opt = goodsRepo.findById(goodsId);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Seckill not active or product not found"));
        }
        SeckillGoods product = opt.get();
        LocalDateTime now = LocalDateTime.now();
        if (!(product.getStartTime().isBefore(now) || product.getStartTime().isEqual(now)) || product.getEndTime().isBefore(now)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Seckill not active or product not found"));
        }
        Map<String, Object> result = orderEventService.processSeckill(userId, product, req.getAttribute("requestId"));
        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            if ("Product out of stock".equals(result.get("message"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(500).body(result);
        }
    }
}

