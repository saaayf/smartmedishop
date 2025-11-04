package com.smartmedishop.dto;

import java.time.LocalDateTime;

import com.smartmedishop.entity.StockMovement;

public class StockMovementDto {
    public Long id;
    public Long productId;
    public String movementType;
    public Integer quantity;
    public String reason;
    public LocalDateTime createdAt;

    public StockMovementDto() {}

    public StockMovementDto(StockMovement m) {
        this.id = m.getId();
        this.productId = m.getProduct() != null ? m.getProduct().getId() : null;
        this.movementType = m.getMovementType() != null ? m.getMovementType().name() : null;
        this.quantity = m.getQuantity();
        this.reason = m.getReason();
        this.createdAt = m.getCreatedAt();
    }
}
