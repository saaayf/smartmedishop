package com.smartmedishop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartmedishop.entity.Product;
import com.smartmedishop.entity.StockAlert;
import com.smartmedishop.entity.StockAlert.AlertStatus;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    boolean existsByProductAndAlertTypeAndStatus(Product product, String alertType, AlertStatus status);
}
