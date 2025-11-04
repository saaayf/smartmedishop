package com.smartmedishop.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartmedishop.client.RecommendationClient;
import com.smartmedishop.dto.ProductDto;
import com.smartmedishop.entity.Product;
import com.smartmedishop.entity.UserPurchase;
import com.smartmedishop.repository.ProductRepository;
import com.smartmedishop.repository.UserPurchaseRepository;
import com.smartmedishop.repository.UserRepository;

/**
 * Service for product recommendations.
 * Maps Flask API responses (Price/Type pairs) to actual Product entities.
 */
@Service
public class RecommendationService {

    @Autowired
    private RecommendationClient recommendationClient;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserPurchaseRepository userPurchaseRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get general recommendations based on state, type, and price.
     * Returns Product entities that match the recommended Type and Price.
     */
    public List<ProductDto> getRecommendations(String state, String type, Double price, Integer topN) {
        try {
            // Call Flask API
            Map<String, Object> response = recommendationClient.getRecommendations(state, type, price, topN);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) response.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                return new ArrayList<>();
            }

            // Map Flask recommendations to Product entities
            List<ProductDto> products = new ArrayList<>();
            
            for (Map<String, Object> rec : recommendations) {
                // Flask returns: { "Price": 999, "Type": "Pansement médical" }
                Object priceObj = rec.get("Price");
                Object typeObj = rec.get("Type");
                
                if (priceObj == null || typeObj == null) continue;
                
                String recType = typeObj.toString();
                
                // Handle price conversion
                final Double finalRecPrice;
                if (priceObj instanceof Number) {
                    finalRecPrice = ((Number) priceObj).doubleValue();
                } else {
                    try {
                        finalRecPrice = Double.parseDouble(priceObj.toString());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
                
                final String finalRecType = recType;
                
                // Find products matching the type and similar price (within 10% tolerance)
                List<Product> matchingProducts = productRepository.findAll().stream()
                    .filter(p -> p.getType() != null && p.getType().equalsIgnoreCase(finalRecType))
                    .filter(p -> {
                        if (p.getPrice() == null) return false;
                        // Allow 10% price difference
                        double tolerance = finalRecPrice * 0.1;
                        return Math.abs(p.getPrice() - finalRecPrice) <= tolerance;
                    })
                    .collect(Collectors.toList());
                
                // If no exact match, try just by type
                if (matchingProducts.isEmpty()) {
                    matchingProducts = productRepository.findAll().stream()
                        .filter(p -> p.getType() != null && p.getType().equalsIgnoreCase(finalRecType))
                        .collect(Collectors.toList());
                }
                
                // Add matching products to result
                for (Product product : matchingProducts) {
                    if (products.stream().noneMatch(p -> p.id.equals(product.getId()))) {
                        products.add(new ProductDto(product));
                    }
                }
            }
            
            // Limit to topN
            return products.stream().limit(topN != null ? topN : 10).collect(Collectors.toList());
            
        } catch (Exception e) {
            // Return empty list on error
            return new ArrayList<>();
        }
    }

    /**
     * Get user-specific recommendations.
     * Returns Product entities based on user's purchase history.
     * Uses both CSV data (if available) and recent purchases from database.
     */
    public List<ProductDto> getUserRecommendations(String username, Integer topN) {
        try {
            System.out.println("DEBUG RecommendationService: Getting recommendations for user: " + username);
            
            // First, try to get user ID from username
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("DEBUG RecommendationService: User not found: " + username);
            }
            Long userId = userOpt.map(u -> u.getId()).orElse(null);
            System.out.println("DEBUG RecommendationService: User ID for " + username + ": " + userId);
            
            // Get user's purchase history from database
            List<Map<String, Object>> userPurchaseHistory = new ArrayList<>();
            if (userId != null) {
                List<UserPurchase> purchases = userPurchaseRepository.findByUserIdOrderByPurchaseDateDesc(userId);
                System.out.println("DEBUG RecommendationService: Found " + purchases.size() + " purchases for user: " + username + " (userId: " + userId + ")");
                
                if (purchases.isEmpty()) {
                    System.out.println("DEBUG RecommendationService: No purchases found in database for user: " + username);
                } else {
                    for (UserPurchase purchase : purchases) {
                        // Normalize state (e.g., "US" -> "USA")
                        String state = normalizeState(purchase.getState());
                        String type = purchase.getType() != null ? purchase.getType() : "";
                        Double price = purchase.getPrice() != null ? purchase.getPrice().doubleValue() : 0.0;
                        String stockId = purchase.getStockId() != null ? purchase.getStockId().toString() : "";
                        String marque = purchase.getMarque() != null ? purchase.getMarque() : "";
                        
                        System.out.println("DEBUG RecommendationService: Processing purchase - StockId: " + stockId + ", State: " + state + ", Type: " + type + ", Price: " + price);
                        
                        Map<String, Object> purchaseData = new java.util.HashMap<>();
                        purchaseData.put("State", state);
                        purchaseData.put("Type", type);
                        purchaseData.put("Price", price);
                        purchaseData.put("StockId", stockId);
                        purchaseData.put("Marque", marque);
                        
                        userPurchaseHistory.add(purchaseData);
                    }
                    System.out.println("DEBUG RecommendationService: Total purchase history items: " + userPurchaseHistory.size());
                }
            } else {
                System.out.println("DEBUG RecommendationService: userId is null, cannot fetch purchases");
            }
            
            System.out.println("DEBUG RecommendationService: Sending purchase_history to Flask API - size: " + userPurchaseHistory.size());
            
            // Call Flask API with username and purchase history
            Map<String, Object> response = recommendationClient.getUserRecommendations(username, topN, userPurchaseHistory);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) response.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                return new ArrayList<>();
            }

            // Map Flask recommendations to Product entities
            List<ProductDto> products = new ArrayList<>();
            
            for (Map<String, Object> rec : recommendations) {
                // Flask returns: { "Price": 999, "Type": "Pansement médical" }
                Object priceObj = rec.get("Price");
                Object typeObj = rec.get("Type");
                
                if (priceObj == null || typeObj == null) continue;
                
                String recType = typeObj.toString();
                
                // Handle price conversion
                final Double finalRecPrice;
                if (priceObj instanceof Number) {
                    finalRecPrice = ((Number) priceObj).doubleValue();
                } else {
                    try {
                        finalRecPrice = Double.parseDouble(priceObj.toString());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
                
                final String finalRecType = recType;
                
                // Find products matching the type and similar price (within 10% tolerance)
                List<Product> matchingProducts = productRepository.findAll().stream()
                    .filter(p -> p.getType() != null && p.getType().equalsIgnoreCase(finalRecType))
                    .filter(p -> {
                        if (p.getPrice() == null) return false;
                        // Allow 10% price difference
                        double tolerance = finalRecPrice * 0.1;
                        return Math.abs(p.getPrice() - finalRecPrice) <= tolerance;
                    })
                    .collect(Collectors.toList());
                
                // If no exact match, try just by type
                if (matchingProducts.isEmpty()) {
                    matchingProducts = productRepository.findAll().stream()
                        .filter(p -> p.getType() != null && p.getType().equalsIgnoreCase(finalRecType))
                        .collect(Collectors.toList());
                }
                
                // Add matching products to result
                for (Product product : matchingProducts) {
                    if (products.stream().noneMatch(p -> p.id.equals(product.getId()))) {
                        products.add(new ProductDto(product));
                    }
                }
            }
            
            // Limit to topN
            return products.stream().limit(topN != null ? topN : 10).collect(Collectors.toList());
            
        } catch (Exception e) {
            // Return empty list on error
            return new ArrayList<>();
        }
    }

    /**
     * Normalize state values to match CSV format
     * Maps common variations to CSV state names
     */
    private String normalizeState(String state) {
        if (state == null || state.isEmpty()) {
            return "USA";
        }
        
        // Map common variations
        String normalized = state.trim();
        if (normalized.equalsIgnoreCase("US") || normalized.equalsIgnoreCase("United States")) {
            return "USA";
        }
        if (normalized.equalsIgnoreCase("UK") || normalized.equalsIgnoreCase("United Kingdom")) {
            return "Angleterre";
        }
        
        // Valid states from CSV: Angleterre, Australie, Brazil, Canada, France, Germany, India, Japon, Kenya, Nigeria, Scotland, USA
        String[] validStates = {"Angleterre", "Australie", "Brazil", "Canada", "France", "Germany", "India", "Japon", "Kenya", "Nigeria", "Scotland", "USA"};
        for (String validState : validStates) {
            if (validState.equalsIgnoreCase(normalized)) {
                return validState;
            }
        }
        
        // Default to USA if not found
        return "USA";
    }
}

