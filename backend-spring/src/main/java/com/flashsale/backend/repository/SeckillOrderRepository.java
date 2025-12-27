package com.flashsale.backend.repository;

import com.flashsale.backend.model.SeckillOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeckillOrderRepository extends JpaRepository<SeckillOrder, Long> {

    @Query(value = "SELECT COUNT(*) as total FROM seckill_orders o WHERE o.user_id = :userId AND (:status IS NULL OR o.status = :status)", nativeQuery = true)
    long countByUserAndStatus(@Param("userId") long userId, @Param("status") Integer status);

    @Query(value = "SELECT o.*, g.name as goods_name, g.original_price, g.seckill_price, g.description FROM seckill_orders o JOIN seckill_goods g ON o.goods_id = g.id WHERE o.user_id = :userId AND (:status IS NULL OR o.status = :status) ORDER BY o.create_time DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Object[]> findOrdersWithGoods(@Param("userId") long userId, @Param("status") Integer status, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT o.*, g.name as goods_name, g.original_price, g.seckill_price, g.description FROM seckill_orders o JOIN seckill_goods g ON o.goods_id = g.id WHERE o.id = :id AND o.user_id = :userId", nativeQuery = true)
    List<Object[]> findOrderByIdWithGoods(@Param("id") long id, @Param("userId") long userId);

    java.util.Optional<SeckillOrder> findByExternalOrderId(String externalOrderId);
}

