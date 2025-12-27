package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.repository.SeckillOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final SeckillOrderRepository orderRepo;
    private final LoggerService log;

    public OrderController(SeckillOrderRepository orderRepo, LoggerService log) {
        this.orderRepo = orderRepo;
        this.log = log;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                  @RequestParam(name = "limit", defaultValue = "10") int limit,
                                  @RequestParam(name = "status", required = false) Integer status,
                                  HttpServletRequest req) {
        var auth = (org.springframework.security.core.Authentication) req.getUserPrincipal();
        if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        Object principal = auth.getPrincipal();
        long userId = principal instanceof java.util.Map<?, ?> m ? ((Number) m.get("userId")).longValue() : -1;
        if (userId <= 0) return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        int offset = Math.max(0, (page - 1) * limit);
        long total = orderRepo.countByUserAndStatus(userId, status);
        List<Object[]> rows = orderRepo.findOrdersWithGoods(userId, status, limit, offset);
        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
        ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
        ctx.put("userId", userId);
        ctx.put("page", page);
        ctx.put("limit", limit);
        ctx.put("total", total);
        log.info("orders_fetched", ctx);
        List<Map<String, Object>> items = new java.util.ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Map<String, Object> o = new java.util.LinkedHashMap<>();
            o.put("id", r[0]);
            o.put("user_id", r[1]);
            o.put("goods_id", r[2]);
            o.put("create_time", r[3]);
            o.put("status", r[4]);
            o.put("updated_at", r[5]);
            o.put("goods_name", r[6]);
            o.put("original_price", r[7]);
            o.put("seckill_price", r[8]);
            o.put("description", r[9]);
            items.add(o);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("orders", items);
        resp.put("total", total);
        resp.put("page", page);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> byId(@PathVariable("id") long id, HttpServletRequest req) {
        var auth = (org.springframework.security.core.Authentication) req.getUserPrincipal();
        if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        Object principal = auth.getPrincipal();
        long userId = principal instanceof java.util.Map<?, ?> m ? ((Number) m.get("userId")).longValue() : -1;
        if (userId <= 0) return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        List<Object[]> rows = orderRepo.findOrderByIdWithGoods(id, userId);
        if (rows.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
        Object[] r = rows.get(0);
        Map<String, Object> o = new java.util.LinkedHashMap<>();
        o.put("id", r[0]);
        o.put("user_id", r[1]);
        o.put("goods_id", r[2]);
        o.put("create_time", r[3]);
        o.put("status", r[4]);
        o.put("updated_at", r[5]);
        o.put("goods_name", r[6]);
        o.put("original_price", r[7]);
        o.put("seckill_price", r[8]);
        o.put("description", r[9]);
        return ResponseEntity.ok(Map.of("order", o));
    }
}

