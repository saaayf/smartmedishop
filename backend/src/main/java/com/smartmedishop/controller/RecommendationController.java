package com.smartmedishop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.dto.ProductDto;
import com.smartmedishop.security.CustomUserDetailsService;
import com.smartmedishop.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * Get general product recommendations based on state, type, and price
     * GET /api/recommendations?state=Angleterre&type=Pansement médical&price=999&top_n=10
     */
    @GetMapping
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false, defaultValue = "10") Integer top_n) {
        
        try {
            List<ProductDto> recommendations = recommendationService.getRecommendations(
                state, type, price, top_n
            );
            
            return ResponseEntity.ok(Map.of(
                "recommendations", recommendations,
                "count", recommendations.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get recommendations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get user-specific recommendations based on purchase history
     * GET /api/recommendations/user?top_n=10
     * Requires authentication
     */
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserRecommendations(
            @RequestParam(required = false, defaultValue = "10") Integer top_n,
            Authentication authentication) {
        
        System.out.println("DEBUG RecommendationController: /user endpoint called");
        System.out.println("DEBUG RecommendationController: top_n = " + top_n);
        System.out.println("DEBUG RecommendationController: authentication = " + (authentication != null ? "present" : "null"));
        
        try {
            if (authentication == null) {
                System.out.println("DEBUG RecommendationController: Authentication is null, returning 401");
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            // Get username from authentication
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            String username = userDetails.getUser().getUsername();
            System.out.println("DEBUG RecommendationController: Username from authentication: " + username);
            
            System.out.println("DEBUG RecommendationController: Calling recommendationService.getUserRecommendations()");
            List<ProductDto> recommendations = recommendationService.getUserRecommendations(username, top_n);
            System.out.println("DEBUG RecommendationController: Got " + recommendations.size() + " recommendations");
            
            return ResponseEntity.ok(Map.of(
                "username", username,
                "recommendations", recommendations,
                "count", recommendations.size()
            ));
        } catch (Exception e) {
            System.out.println("DEBUG RecommendationController: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get user recommendations: " + e.getMessage()
            ));
        }
    }
}

