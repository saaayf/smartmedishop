package com.smartmedishop.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmedishop.entity.Product;
import com.smartmedishop.entity.StockAlert;
import com.smartmedishop.entity.StockMovement;
import com.smartmedishop.repository.ProductRepository;
import com.smartmedishop.repository.StockAlertRepository;
import com.smartmedishop.repository.StockMovementRepository;

@Service
public class StockService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private StockAlertRepository alertRepository;

    // CRUD product
    public Product createProduct(Product p) { 
        Product saved = productRepository.save(p);
        checkAndCreateAlerts(saved);
        return saved;
    }
    
    public Optional<Product> findById(Long id) { return productRepository.findById(id); }
    public List<Product> findAll() { return productRepository.findAll(); }
    
    public Product updateProduct(Product p) { 
        Product updated = productRepository.save(p);
        checkAndCreateAlerts(updated);
        return updated;
    }
    
    public void deleteProduct(Long id) { productRepository.deleteById(id); }

    // Movements
    @Transactional
    public StockMovement recordMovement(Product product, StockMovement.MovementType type, Integer qty, String reason) {
        StockMovement m = new StockMovement(product, type, qty, reason);
        // Adjust quantity
        if (type == StockMovement.MovementType.IN) {
            product.setQuantity(product.getQuantity() + qty);
        } else {
            product.setQuantity(Math.max(0, product.getQuantity() - qty));
        }
        productRepository.save(product);
        StockMovement saved = movementRepository.save(m);

        // Check alerts
        checkAndCreateAlerts(product);

        return saved;
    }

    public List<StockMovement> getMovementsForProduct(Long productId) {
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<StockAlert> getAlertsForProduct(Long productId) {
        return alertRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    // Record a sale (OUT movement)
    @Transactional
    public void recordSaleBySku(String sku, Integer qty, String reason) throws Exception {
        Product p = productRepository.findBySku(sku);
        if (p == null) throw new Exception("Product not found for sku: " + sku);
        recordMovement(p, StockMovement.MovementType.OUT, qty, reason != null ? reason : "SALE");
    }

    // Check low stock and expiration
    private void checkAndCreateAlerts(Product product) {
        // Low stock
        int threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 5;
        if (product.getQuantity() <= threshold) {
            // Vérifie qu'une alerte LOW_STOCK ACTIVE n'existe pas déjà
            boolean alertExists = alertRepository.existsByProductAndAlertTypeAndStatus(
                product, "LOW_STOCK", StockAlert.AlertStatus.ACTIVE
            );
            if (!alertExists) {
                StockAlert alert = new StockAlert(
                    product, 
                    "LOW_STOCK", 
                    "Product " + product.getSku() + " low stock: " + product.getQuantity() + " (threshold: " + threshold + ")"
                );
                alertRepository.save(alert);
            }
        }

        // Expiration
        if (product.getExpirationDate() != null) {
            if (!product.getExpirationDate().isAfter(LocalDate.now())) {
                // Vérifie qu'une alerte EXPIRED ACTIVE n'existe pas déjà
                boolean alertExists = alertRepository.existsByProductAndAlertTypeAndStatus(
                    product, "EXPIRED", StockAlert.AlertStatus.ACTIVE
                );
                if (!alertExists) {
                    StockAlert alert = new StockAlert(
                        product, 
                        "EXPIRED", 
                        "Product " + product.getSku() + " expired on " + product.getExpirationDate()
                    );
                    alertRepository.save(alert);
                }
            }
        }
    }
}
