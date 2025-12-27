package com.flashsale.backend.jobs;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.SeckillGoods;
import com.flashsale.backend.repository.SeckillGoodsRepository;
import com.flashsale.backend.service.CacheService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GoodsCleanupJob {
    private final SeckillGoodsRepository goodsRepo;
    private final StringRedisTemplate redis;
    private final CacheService cacheService;
    private final LoggerService log;

    public GoodsCleanupJob(SeckillGoodsRepository goodsRepo, StringRedisTemplate redis, CacheService cacheService, LoggerService log) {
        this.goodsRepo = goodsRepo;
        this.redis = redis;
        this.cacheService = cacheService;
        this.log = log;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void cleanupExpiredAndSoldOut() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillGoods> goods = goodsRepo.findAll();
        List<Long> deletedIds = new ArrayList<>();
        for (SeckillGoods g : goods) {
            Long id = g.getId();
            boolean expired = g.getEndTime() != null && !g.getEndTime().isAfter(now);
            int stockVal = -1;
            try {
                String sv = redis.opsForValue().get("seckill:stock:" + id);
                if (sv != null) {
                    stockVal = Integer.parseInt(sv);
                } else if (g.getStock() != null) {
                    stockVal = g.getStock();
                }
            } catch (Exception ignored) {}
            boolean soldOut = stockVal <= 0;
            if (expired || soldOut) {
                try {
                    goodsRepo.deleteById(id);
                } catch (Exception e) {
                    log.error("goods_cleanup_db_error", Map.of("id", id, "error", e.getMessage()));
                    continue;
                }
                try {
                    redis.delete("seckill:stock:" + id);
                } catch (Exception ignored) {}
                try {
                    redis.delete("product:" + id);
                } catch (Exception ignored) {}
                try {
                    redis.delete("goods:sold_count:" + id);
                } catch (Exception ignored) {}
                try {
                    redis.opsForZSet().remove("goods:active_by_stock", String.valueOf(id));
                } catch (Exception ignored) {}
                deletedIds.add(id);
                log.info("goods_cleanup_deleted", Map.of("id", id, "expired", expired, "stock", stockVal));
            }
        }
        if (!deletedIds.isEmpty()) {
            cacheService.invalidateProductListCaches();
        }
        log.info("goods_cleanup_heartbeat", Map.of("checked", goods.size(), "deleted", deletedIds.size()));
    }
}
