package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private final StringRedisTemplate redis;
    private final LoggerService log;
    @PersistenceContext
    private EntityManager em;

    public BusinessController(StringRedisTemplate redis, LoggerService log) {
        this.redis = redis;
        this.log = log;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        String turnoverStr = redis.opsForValue().get("mall:turnover");
        double turnover = 0.0;
        try { turnover = Double.parseDouble(turnoverStr == null ? "0" : turnoverStr); } catch (Exception ignored) {}
        var q = em.createNativeQuery("SELECT COUNT(*) as total_sold FROM seckill_orders WHERE status = 2");
        Number totalSoldNum = (Number) q.getSingleResult();
        long totalSold = totalSoldNum == null ? 0 : totalSoldNum.longValue();
        log.info("business_overview", Map.of("turnover", turnover, "totalSold", totalSold));
        return ResponseEntity.ok(Map.of("turnover", turnover, "total_sold", totalSold));
    }

    @GetMapping("/sales")
    public ResponseEntity<?> sales() {
        String sql = "SELECT g.id as goods_id, g.name as goods_name, g.seckill_price, COUNT(o.id) as sold_count FROM seckill_goods g LEFT JOIN seckill_orders o ON o.goods_id = g.id AND o.status = 2 GROUP BY g.id, g.name, g.seckill_price ORDER BY sold_count DESC";
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        List<Map<String, Object>> items = new java.util.ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Map<String, Object> o = new java.util.LinkedHashMap<>();
            o.put("goods_id", r[0]);
            o.put("goods_name", r[1]);
            o.put("seckill_price", r[2]);
            o.put("sold_count", r[3]);
            items.add(o);
        }
        log.info("business_sales_fetched", Map.of("count", items.size()));
        return ResponseEntity.ok(Map.of("items", items));
    }
}

