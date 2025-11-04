package com.smartmedishop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartmedishop.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<StockMovement> findByProductIdAndMovementTypeOrderByCreatedAtDesc(Long productId, StockMovement.MovementType movementType);
}
