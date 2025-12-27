package com.flashsale.backend.repository;

import com.flashsale.backend.model.SeckillGoods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillGoodsRepository extends JpaRepository<SeckillGoods, Long> {

    @Query(value = "SELECT COUNT(*) FROM seckill_goods WHERE (:status = 'all') OR (:status = 'upcoming' AND start_time > :now) OR (:status = 'active' AND start_time <= :now AND end_time > :now) OR (:status = 'ended' AND end_time <= :now)", nativeQuery = true)
    long countByStatus(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query(value = "SELECT * FROM seckill_goods WHERE (:status = 'all') OR (:status = 'upcoming' AND start_time > :now) OR (:status = 'active' AND start_time <= :now AND end_time > :now) OR (:status = 'ended' AND end_time <= :now) ORDER BY start_time ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<SeckillGoods> findPageByStatus(@Param("status") String status, @Param("now") LocalDateTime now, @Param("limit") int limit, @Param("offset") int offset);
}

