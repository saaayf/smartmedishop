package com.smartmedishop.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmedishop.entity.Product;
import com.smartmedishop.entity.StockAlert;
import com.smartmedishop.repository.ProductRepository;
import com.smartmedishop.repository.StockAlertRepository;

@Service
public class AlertMigrationService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockAlertRepository alertRepository;

    /**
     * Parcourt tous les produits existants et crée les alertes manquantes
     * pour ceux qui ont un stock bas ou qui sont expirés
     * 
     * @return Un résumé du nombre d'alertes créées
     */
    @Transactional
    public Map<String, Object> generateMissingAlerts() {
        List<Product> allProducts = productRepository.findAll();
        
        int lowStockAlertsCreated = 0;
        int expiredAlertsCreated = 0;
        int productsChecked = 0;

        for (Product product : allProducts) {
            productsChecked++;
            
            // Vérifier le stock bas
            int threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 5;
            if (product.getQuantity() <= threshold) {
                boolean alertExists = alertRepository.existsByProductAndAlertTypeAndStatus(
                    product, "LOW_STOCK", StockAlert.AlertStatus.ACTIVE
                );
                
                if (!alertExists) {
                    StockAlert alert = new StockAlert(
                        product,
                        "LOW_STOCK",
                        "Product " + product.getSku() + " low stock: " + product.getQuantity() + 
                        " (threshold: " + threshold + ")"
                    );
                    alertRepository.save(alert);
                    lowStockAlertsCreated++;
                }
            }

            // Vérifier l'expiration
            if (product.getExpirationDate() != null && !product.getExpirationDate().isAfter(LocalDate.now())) {
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
                    expiredAlertsCreated++;
                }
            }
        }

        // Retourner un résumé
        Map<String, Object> summary = new HashMap<>();
        summary.put("productsChecked", productsChecked);
        summary.put("lowStockAlertsCreated", lowStockAlertsCreated);
        summary.put("expiredAlertsCreated", expiredAlertsCreated);
        summary.put("totalAlertsCreated", lowStockAlertsCreated + expiredAlertsCreated);
        summary.put("message", "Migration completed successfully");

        return summary;
    }
}
