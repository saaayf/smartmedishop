package com.smartmedishop.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.entity.User;
import com.smartmedishop.entity.UserBehavior;
import com.smartmedishop.repository.UserBehaviorRepository;
import com.smartmedishop.security.CustomUserDetailsService;

@RestController
@RequestMapping("/api/user-behavior")
@CrossOrigin(origins = "*")
public class UserBehaviorController {
    
    @Autowired
    private UserBehaviorRepository userBehaviorRepository;
    
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserBehavior(@PathVariable Long userId, Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User currentUser = userDetails.getUser();
            
            // Check if user is accessing their own behavior or is ADMIN/FRAUD_ANALYST
            if (!currentUser.getId().equals(userId) && 
                !currentUser.getUserType().equals(User.UserType.ADMIN) && 
                !currentUser.getUserType().equals(User.UserType.FRAUD_ANALYST)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. You can only view your own user behavior."));
            }
            
            Optional<UserBehavior> userBehaviorOpt = userBehaviorRepository.findByUserId(userId);
            
            if (userBehaviorOpt.isPresent()) {
                UserBehavior userBehavior = userBehaviorOpt.get();
                
                Map<String, Object> response = new HashMap<>();
                response.put("id", userBehavior.getId());
                response.put("userId", userBehavior.getUserId());
                response.put("transactionVelocity", userBehavior.getTransactionVelocity());
                response.put("amountVelocity", userBehavior.getAmountVelocity());
                response.put("averageTransactionAmount", userBehavior.getAverageTransactionAmount());
                response.put("maxTransactionAmount", userBehavior.getMaxTransactionAmount());
                response.put("minTransactionAmount", userBehavior.getMinTransactionAmount());
                response.put("lastTransactionDate", userBehavior.getLastTransactionDate());
                response.put("transactionFrequencyPerDay", userBehavior.getTransactionFrequencyPerDay());
                response.put("weekendTransactionRatio", userBehavior.getWeekendTransactionRatio());
                response.put("nightTransactionRatio", userBehavior.getNightTransactionRatio());
                response.put("preferredPaymentMethod", userBehavior.getPreferredPaymentMethod());
                response.put("preferredDeviceType", userBehavior.getPreferredDeviceType());
                response.put("locationCountry", userBehavior.getLocationCountry());
                response.put("unusualPatternsCount", userBehavior.getUnusualPatternsCount());
                response.put("createdAt", userBehavior.getCreatedAt());
                response.put("lastUpdated", userBehavior.getLastUpdated());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("message", "No user behavior found for user ID: " + userId));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get user behavior: " + e.getMessage()));
        }
    }
    
    // Get all user behaviors (for FRAUD_ANALYST and ADMIN only)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getAllUserBehaviors() {
        try {
            List<UserBehavior> userBehaviors = userBehaviorRepository.findAll();
            
            // Create response with all user behavior data
            List<Map<String, Object>> responseList = userBehaviors.stream()
                .map(userBehavior -> {
                    Map<String, Object> behavior = new HashMap<>();
                    behavior.put("id", userBehavior.getId());
                    behavior.put("userId", userBehavior.getUserId());
                    behavior.put("transactionVelocity", userBehavior.getTransactionVelocity());
                    behavior.put("amountVelocity", userBehavior.getAmountVelocity());
                    behavior.put("averageTransactionAmount", userBehavior.getAverageTransactionAmount());
                    behavior.put("maxTransactionAmount", userBehavior.getMaxTransactionAmount());
                    behavior.put("minTransactionAmount", userBehavior.getMinTransactionAmount());
                    behavior.put("lastTransactionDate", userBehavior.getLastTransactionDate());
                    behavior.put("transactionFrequencyPerDay", userBehavior.getTransactionFrequencyPerDay());
                    behavior.put("weekendTransactionRatio", userBehavior.getWeekendTransactionRatio());
                    behavior.put("nightTransactionRatio", userBehavior.getNightTransactionRatio());
                    behavior.put("preferredPaymentMethod", userBehavior.getPreferredPaymentMethod());
                    behavior.put("preferredDeviceType", userBehavior.getPreferredDeviceType());
                    behavior.put("locationCountry", userBehavior.getLocationCountry());
                    behavior.put("unusualPatternsCount", userBehavior.getUnusualPatternsCount());
                    behavior.put("createdAt", userBehavior.getCreatedAt());
                    behavior.put("lastUpdated", userBehavior.getLastUpdated());
                    return behavior;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("userBehaviors", responseList);
            response.put("total", userBehaviors.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get all user behaviors: " + e.getMessage()));
        }
    }
}
