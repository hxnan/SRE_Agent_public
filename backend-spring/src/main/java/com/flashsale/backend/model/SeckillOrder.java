package com.flashsale.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seckill_orders")
public class SeckillOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "status")
    private Integer status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "external_order_id", unique = true)
    private String externalOrderId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
}

