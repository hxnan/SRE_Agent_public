package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.SeckillGoods;
import com.flashsale.backend.repository.SeckillGoodsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final SeckillGoodsRepository goodsRepo;
    private final StringRedisTemplate redis;
    private final LoggerService log;
    private final EntityManager em;
    private final ObjectMapper mapper;

    public ProductController(SeckillGoodsRepository goodsRepo, StringRedisTemplate redis, LoggerService log, EntityManager em, ObjectMapper mapper) {
        this.goodsRepo = goodsRepo;
        this.redis = redis;
        this.log = log;
        this.em = em;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                  @RequestParam(name = "limit", defaultValue = "10") int limit,
                                  @RequestParam(name = "status", required = false) String status,
                                  @RequestParam(name = "sort_by", required = false) String sortBy,
                                  @RequestParam(name = "sort_order", required = false) String sortOrder,
                                  @RequestParam(name = "expired", required = false) Boolean expired,
                                  @RequestParam(name = "available", required = false) Boolean available,
                                  HttpServletRequest req) {
        int offset = Math.max(0, (page - 1) * limit);
        String s = (status == null || status.isBlank()) ? "all" : status;
        String sb = (sortBy == null || sortBy.isBlank()) ? "start_time" : sortBy;
        String so = (sortOrder == null || sortOrder.isBlank()) ? "asc" : sortOrder;
        String cacheKey = String.format("products:%d:%d:%s:%s:%s:%s:%s", page, limit, s, sb, so, expired == null ? "null" : expired.toString(), available == null ? "null" : available.toString());
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            java.util.Map<String,Object> ctx = new java.util.HashMap<>();
            ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
            ctx.put("page", page);
            ctx.put("limit", limit);
            ctx.put("status", s);
            ctx.put("sort_by", sb);
            ctx.put("sort_order", so);
            ctx.put("expired", expired);
            ctx.put("available", available);
            log.info("products_cache_hit", ctx);
            try {
                var node = mapper.readTree(cached);
                return ResponseEntity.ok(node);
            } catch (Exception ignore) {}
        }
        LocalDateTime now = LocalDateTime.now();
        if ("active".equals(s)) {
            try {
                Long total = redis.opsForZSet().zCard("goods:active_by_stock");
                total = total == null ? 0L : total;
                java.util.Set<String> ids = redis.opsForZSet().reverseRange("goods:active_by_stock", offset, Math.max(0, offset + limit - 1));
                java.util.List<SeckillGoods> items = new java.util.ArrayList<>();
                if (ids != null) {
                    for (String idStr : ids) {
                        long id = Long.parseLong(idStr);
                        SeckillGoods g = null;
                        String pj = redis.opsForValue().get("product:" + id);
                        if (pj != null) {
                            try { g = mapper.readValue(pj, SeckillGoods.class); } catch (Exception ignored) {}
                        }
                        if (g == null) {
                            var opt = goodsRepo.findById(id);
                            if (opt.isPresent()) {
                                g = opt.get();
                                try {
                                    String json = mapper.writeValueAsString(g);
                                    redis.opsForValue().set("product:" + id, json);
                                } catch (Exception ignored) {}
                            }
                        }
                        if (g != null) {
                            String sv = redis.opsForValue().get("seckill:stock:" + id);
                            if (sv != null) {
                                try { g.setStock(Integer.parseInt(sv)); } catch (Exception ignored) {}
                            }
                            items.add(g);
                        }
                    }
                }
                if (Boolean.TRUE.equals(available)) {
                    items.removeIf(g -> {
                        Integer st = g.getStock();
                        return st == null || st <= 0;
                    });
                    total = (long) items.size();
                }
                Map<String, Object> resp = Map.of("products", items, "total", total, "page", page);
                String json = mapper.writeValueAsString(resp);
                redis.opsForValue().set(cacheKey, json, java.time.Duration.ofSeconds(2));
                java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
                ctx.put("page", page);
                ctx.put("limit", limit);
                ctx.put("status", s);
                ctx.put("sort_by", sb);
                ctx.put("sort_order", so);
                ctx.put("expired", expired);
                ctx.put("available", available);
                ctx.put("total", total);
                log.info("products_fetched", ctx);
                return ResponseEntity.ok(resp);
            } catch (Exception e) {
                java.util.Map<String,Object> err = new java.util.HashMap<>();
                err.put("error", String.valueOf(e.getMessage()));
                log.error("products_active_redis_error", err);
                // Fallback to DB path below
            }
        }
        StringBuilder where = new StringBuilder();
        if ("upcoming".equals(s)) {
            where.append(" start_time > :now");
        } else if ("active".equals(s)) {
            where.append(" start_time <= :now AND end_time > :now");
        } else if ("ended".equals(s)) {
            where.append(" end_time <= :now");
        } else {
            where.append(" 1=1");
        }
        if (expired != null) {
            if (expired) {
                where.append(" AND end_time <= :now");
            } else {
                where.append(" AND end_time > :now");
            }
        }
        String col;
        switch (sb) {
            case "created_at" -> col = "created_at";
            case "stock" -> col = "stock";
            case "end_time" -> col = "end_time";
            default -> col = "start_time";
        }
        String ord = "desc".equalsIgnoreCase(so) ? "DESC" : "ASC";
        String countSql = "SELECT COUNT(*) FROM seckill_goods WHERE " + where;
        String listSql = "SELECT * FROM seckill_goods WHERE " + where + " ORDER BY " + col + " " + ord + " LIMIT :limit OFFSET :offset";
        boolean needsNow = where.indexOf(":now") >= 0;
        Number totalNum;
        List<SeckillGoods> list;
        try {
            var countQuery = em.createNativeQuery(countSql);
            if (needsNow) countQuery.setParameter("now", now);
            totalNum = ((Number) countQuery.getSingleResult());
            var q = em.createNativeQuery(listSql, SeckillGoods.class);
            if (needsNow) q.setParameter("now", now);
            q.setParameter("limit", limit);
            q.setParameter("offset", offset);
            list = q.getResultList();
        } catch (Exception e) {
            java.util.Map<String,Object> ctx = new java.util.HashMap<>();
            ctx.put("error", e.getMessage());
            ctx.put("page", page);
            ctx.put("limit", limit);
            ctx.put("status", s);
            ctx.put("sort_by", sb);
            ctx.put("sort_order", so);
            ctx.put("expired", expired);
            ctx.put("available", available);
            ctx.put("count_sql", countSql);
            ctx.put("list_sql", listSql);
            ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
            log.error("products_query_error", ctx);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
        long total = totalNum.longValue();
        java.util.List<com.flashsale.backend.model.SeckillGoods> enriched = new java.util.ArrayList<>(list.size());
        for (com.flashsale.backend.model.SeckillGoods g : list) {
            String sk = "seckill:stock:" + g.getId();
            String sv = redis.opsForValue().get(sk);
            if (sv != null) {
                try { g.setStock(Integer.parseInt(sv)); } catch (Exception ignored) {}
            }
            enriched.add(g);
        }
        if (Boolean.TRUE.equals(available)) {
            enriched.removeIf(g -> {
                Integer st = g.getStock();
                return st == null || st <= 0;
            });
            total = enriched.size();
        }
        Map<String, Object> resp = Map.of("products", enriched, "total", total, "page", page);
        try {
            String json = mapper.writeValueAsString(resp);
            if ("active".equals(s)) {
                redis.opsForValue().set(cacheKey, json, java.time.Duration.ofSeconds(2));
            } else {
                redis.opsForValue().set(cacheKey, json, java.time.Duration.ofSeconds(20));
            }
        } catch (Exception ignored) {}
        java.util.Map<String,Object> ctx2 = new java.util.HashMap<>();
        ctx2.put("requestId", String.valueOf(req.getAttribute("requestId")));
        ctx2.put("page", page);
        ctx2.put("limit", limit);
        ctx2.put("status", s);
        ctx2.put("sort_by", sb);
        ctx2.put("sort_order", so);
        ctx2.put("expired", expired);
        ctx2.put("available", available);
        ctx2.put("total", total);
        log.info("products_fetched", ctx2);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> byId(@PathVariable("id") long id) throws Exception {
        String cacheKey = "product:" + id;
        String cached = null; // always compute live stock below to avoid staleness
        if (cached != null) {
            var node = mapper.readTree(cached);
            return ResponseEntity.ok(node);
        }
        var opt = goodsRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
        }
        var product = opt.get();
        String sv = redis.opsForValue().get("seckill:stock:" + id);
        if (sv != null) {
            try { product.setStock(Integer.parseInt(sv)); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(product);
    }
}
