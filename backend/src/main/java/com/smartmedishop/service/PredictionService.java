package com.smartmedishop.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartmedishop.client.PredictionClient;
import com.smartmedishop.dto.PredictionDto;
import com.smartmedishop.entity.Product;

@Service
public class PredictionService {

    @Autowired
    private PredictionClient predictionClient;

    /**
     * Try to predict using rich features; fallback to product name.
     */
    public PredictionDto predictForProduct(Product p) {
        try {
            Map<String, Object> features = new HashMap<>();
            features.put("name", p.getName());
            features.put("quantity_stock", p.getQuantity());
            features.put("low_stock_threshold", p.getLowStockThreshold());
            features.put("price", p.getPrice());
            // Product entity does not contain category/supplier in this model; use available fields
            features.put("sku", p.getSku());
            if (p.getExpirationDate() != null) features.put("expiration_date", p.getExpirationDate().toString());
            if (p.getCreatedAt() != null) features.put("created_at", p.getCreatedAt().toString());

            PredictionDto dto = predictionClient.predictByFeatures(features);
            // If response empty, fallback to name-based
            if (dto.getProductNameUsed() == null || dto.getProductNameUsed().isEmpty()) {
                dto = predictionClient.predictByProductName(p.getName());
            }

            // Ensure productNameUsed is set
            if (dto.getProductNameUsed() == null || dto.getProductNameUsed().isEmpty()) {
                dto.setProductNameUsed(p.getName());
            }

            // Compute a reasonable recommendedStock if model did not provide it
            if (dto.getRecommendedStock() == 0) {
                int rec = 0;
                Integer lowThreshold = p.getLowStockThreshold();
                Integer qty = p.getQuantity();
                double pred = dto.getPredictedDemand();
                if (lowThreshold != null && lowThreshold > 0) {
                    rec = Math.max(lowThreshold * 2, (int) Math.ceil(pred) + (qty != null ? Math.max(0, qty) : 0));
                } else if (qty != null) {
                    rec = qty + (int) Math.ceil(pred * 1.5) + 5;
                } else {
                    rec = Math.max(1, (int) Math.ceil(pred * 1.5) + 5);
                }
                dto.setRecommendedStock(rec);
            }

            return dto;
        } catch (Exception ex) {
            // fallback: try by product name
            try {
                return predictionClient.predictByProductName(p.getName());
            } catch (Exception e) {
                return new PredictionDto();
            }
        }
    }
}
