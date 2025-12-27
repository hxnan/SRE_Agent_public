package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.SeckillGoods;
import com.flashsale.backend.repository.SeckillGoodsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/products/batch")
public class ProductAdminController {
    private final SeckillGoodsRepository goodsRepo;
    private final StringRedisTemplate redis;
    private final LoggerService log;
    private final ObjectMapper mapper;

    public ProductAdminController(SeckillGoodsRepository goodsRepo, StringRedisTemplate redis, LoggerService log, ObjectMapper mapper) {
        this.goodsRepo = goodsRepo;
        this.redis = redis;
        this.log = log;
        this.mapper = mapper;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> batchCreate(@RequestBody Map<String, Object> body) {
        int count = ((Number) body.getOrDefault("count", 0)).intValue();
        int stockPerItem = ((Number) body.getOrDefault("stock_per_item", 0)).intValue();
        BigDecimal seckillPrice = new BigDecimal(String.valueOf(body.getOrDefault("seckill_price", "0")));
        BigDecimal originalPrice = new BigDecimal(String.valueOf(body.getOrDefault("original_price", "0")));
        String namePrefix = String.valueOf(body.getOrDefault("name_prefix", "压测商品"));
        String description = String.valueOf(body.getOrDefault("description", "用于压测"));
        String startTimeStr = String.valueOf(body.getOrDefault("start_time", null));
        String endTimeStr = String.valueOf(body.getOrDefault("end_time", null));
        if (count <= 0 || stockPerItem <= 0 || startTimeStr == null || endTimeStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid batch create parameters"));
        }
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr.replace("Z", ""));
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr.replace("Z", ""));

        List<SeckillGoods> batch = new ArrayList<>(Math.min(count, 1000));
        List<Long> createdIds = new ArrayList<>(count);
        int chunk = 1000;
        for (int i = 1; i <= count; i++) {
            SeckillGoods g = new SeckillGoods();
            g.setName(namePrefix + "-" + i);
            g.setDescription(description + "-" + i);
            g.setStock(stockPerItem);
            g.setSeckillPrice(seckillPrice);
            g.setOriginalPrice(originalPrice);
            g.setStartTime(startTime);
            g.setEndTime(endTime);
            batch.add(g);
            if (batch.size() >= chunk || i == count) {
                List<SeckillGoods> saved = goodsRepo.saveAll(batch);
                for (SeckillGoods s : saved) {
                    createdIds.add(s.getId());
                    String stockKey = "seckill:stock:" + s.getId();
                    redis.opsForValue().set(stockKey, String.valueOf(s.getStock()));
                    try {
                        String json = mapper.writeValueAsString(s);
                        redis.opsForValue().set("product:" + s.getId(), json);
                    } catch (Exception ignored) {}
                    try {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        if ((s.getStartTime().isBefore(now) || s.getStartTime().isEqual(now)) && s.getEndTime().isAfter(now)) {
                            redis.opsForZSet().add("goods:active_by_stock", String.valueOf(s.getId()), s.getStock());
                        }
                    } catch (Exception ignored) {}
                }
                batch.clear();
            }
        }

        try {
            Set<String> keys = redis.keys("products:*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ignore) {}

        long idStart = createdIds.isEmpty() ? 0 : Collections.min(createdIds);
        long idEnd = createdIds.isEmpty() ? 0 : Collections.max(createdIds);
        log.info("batch_products_created", Map.of("inserted", createdIds.size(), "id_start", idStart, "id_end", idEnd));
        return ResponseEntity.ok(Map.of("inserted", createdIds.size(), "id_start", idStart, "id_end", idEnd));
    }
}
