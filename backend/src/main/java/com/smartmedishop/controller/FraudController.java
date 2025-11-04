package com.smartmedishop.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.entity.FraudAlert;
import com.smartmedishop.entity.Transaction;
import com.smartmedishop.entity.User;
import com.smartmedishop.repository.FraudAlertRepository;
import com.smartmedishop.repository.TransactionRepository;
import com.smartmedishop.security.CustomUserDetailsService;
import com.smartmedishop.service.FraudDetectionService;

@RestController
@RequestMapping("/api/fraud")
@CrossOrigin(origins = "*")
public class FraudController {
    
    @Autowired
    private FraudAlertRepository fraudAlertRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private FraudDetectionService fraudDetectionService;
    
    @GetMapping("/alerts")
    public ResponseEntity<?> getFraudAlerts(Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Check if user has permission to view fraud alerts
            if (!user.getUserType().name().equals("ADMIN") && !user.getUserType().name().equals("FRAUD_ANALYST")) {
                return ResponseEntity.status(403).build();
            }
            
            // Get all fraud alerts with transaction and user data eagerly loaded
            List<FraudAlert> alerts = fraudAlertRepository.findAllWithTransactionAndUser();
            
            // Create response - safely access nested relationships
            List<Map<String, Object>> response = alerts.stream()
                .map(alert -> {
                    Map<String, Object> alertData = new HashMap<>();
                    alertData.put("id", alert.getId());
                    alertData.put("alertType", alert.getAlertType());
                    alertData.put("severity", alert.getSeverity() != null ? alert.getSeverity().name() : null);
                    alertData.put("description", alert.getDescription());
                    alertData.put("status", alert.getStatus() != null ? alert.getStatus().name() : null);
                    alertData.put("fraudScore", alert.getFraudScore());
                    alertData.put("riskFactors", alert.getRiskFactors());
                    alertData.put("createdAt", alert.getCreatedAt());
                    alertData.put("resolvedAt", alert.getResolvedAt());
                    alertData.put("resolvedBy", alert.getResolvedBy());
                    alertData.put("investigationNotes", alert.getInvestigationNotes());
                    
                    // Safely access transaction and user data
                    Transaction transaction = alert.getTransaction();
                    if (transaction != null) {
                        alertData.put("transactionId", transaction.getId());
                        alertData.put("amount", transaction.getAmount());
                        alertData.put("merchantName", transaction.getMerchantName());
                        alertData.put("paymentMethod", transaction.getPaymentMethod());
                        alertData.put("transactionDate", transaction.getTransactionDate());
                        
                        // Access user data
                        User transactionUser = transaction.getUser();
                        if (transactionUser != null) {
                            alertData.put("userId", transactionUser.getId());
                            alertData.put("username", transactionUser.getUsername());
                            alertData.put("email", transactionUser.getEmail());
                        } else {
                            alertData.put("userId", null);
                            alertData.put("username", null);
                            alertData.put("email", null);
                        }
                    } else {
                        alertData.put("transactionId", null);
                        alertData.put("amount", null);
                        alertData.put("merchantName", null);
                        alertData.put("paymentMethod", null);
                        alertData.put("transactionDate", null);
                        alertData.put("userId", null);
                        alertData.put("username", null);
                        alertData.put("email", null);
                    }
                    
                    return alertData;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get fraud alerts: " + e.getMessage()));
        }
    }
    
    @GetMapping("/alerts/{id}")
    public ResponseEntity<?> getFraudAlert(@PathVariable Long id, Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Check if user has permission to view fraud alerts
            if (!user.getUserType().name().equals("ADMIN") && !user.getUserType().name().equals("FRAUD_ANALYST")) {
                return ResponseEntity.status(403).build();
            }
            
            // Get fraud alert
            var alertOpt = fraudAlertRepository.findById(id);
            if (alertOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FraudAlert alert = alertOpt.get();
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", alert.getId());
            response.put("alertType", alert.getAlertType());
            response.put("severity", alert.getSeverity());
            response.put("description", alert.getDescription());
            response.put("status", alert.getStatus());
            response.put("fraudScore", alert.getFraudScore());
            response.put("riskFactors", alert.getRiskFactors());
            response.put("investigationNotes", alert.getInvestigationNotes());
            response.put("resolvedBy", alert.getResolvedBy());
            response.put("createdAt", alert.getCreatedAt());
            response.put("resolvedAt", alert.getResolvedAt());
            response.put("transactionId", alert.getTransaction().getId());
            response.put("userId", alert.getTransaction().getUser().getId());
            response.put("amount", alert.getTransaction().getAmount());
            response.put("paymentMethod", alert.getTransaction().getPaymentMethod());
            response.put("transactionDate", alert.getTransaction().getTransactionDate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get fraud alert: " + e.getMessage()));
        }
    }
    
    @PutMapping("/alerts/{id}/resolve")
    public ResponseEntity<?> resolveFraudAlert(@PathVariable Long id, 
                                           @RequestBody Map<String, String> request,
                                           Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Check if user has permission to resolve fraud alerts - Only FRAUD_ANALYST can resolve
            if (!user.getUserType().name().equals("FRAUD_ANALYST")) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Only fraud analysts can resolve fraud alerts"));
            }
            
            // Get fraud alert
            var alertOpt = fraudAlertRepository.findById(id);
            if (alertOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FraudAlert alert = alertOpt.get();
            alert.setStatus(FraudAlert.AlertStatus.RESOLVED);
            alert.setResolvedBy(user.getUsername());
            alert.setResolvedAt(LocalDateTime.now());
            alert.setInvestigationNotes(request.get("investigationNotes"));
            
            fraudAlertRepository.save(alert);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", alert.getId());
            response.put("status", alert.getStatus());
            response.put("resolvedBy", alert.getResolvedBy());
            response.put("resolvedAt", alert.getResolvedAt());
            response.put("message", "Fraud alert resolved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to resolve fraud alert: " + e.getMessage()));
        }
    }
    
    @GetMapping("/suspicious-transactions")
    public ResponseEntity<?> getSuspiciousTransactions(Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Check if user has permission to view suspicious transactions
            if (!user.getUserType().name().equals("ADMIN") && !user.getUserType().name().equals("FRAUD_ANALYST")) {
                return ResponseEntity.status(403).build();
            }
            
            // Get suspicious transactions
            List<Transaction> transactions = transactionRepository.findSuspiciousTransactions();
            
            // Create response
            List<Map<String, Object>> response = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("id", transaction.getId());
                    tx.put("amount", transaction.getAmount());
                    tx.put("paymentMethod", transaction.getPaymentMethod());
                    tx.put("fraudScore", transaction.getFraudScore());
                    tx.put("riskLevel", transaction.getRiskLevel());
                    tx.put("isFraud", transaction.getIsFraud());
                    tx.put("fraudReasons", transaction.getFraudReasons());
                    tx.put("transactionDate", transaction.getTransactionDate());
                    tx.put("userId", transaction.getUser().getId());
                    tx.put("username", transaction.getUser().getUsername());
                    tx.put("merchantName", transaction.getMerchantName());
                    return tx;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get suspicious transactions: " + e.getMessage()));
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<?> getFraudStatistics(Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Check if user has permission to view fraud statistics
            if (!user.getUserType().name().equals("ADMIN") && !user.getUserType().name().equals("FRAUD_ANALYST")) {
                return ResponseEntity.status(403).build();
            }
            
            // Get statistics
            Long totalAlerts = fraudAlertRepository.count();
            Long activeAlerts = fraudAlertRepository.countAlertsByStatus(FraudAlert.AlertStatus.ACTIVE);
            Long highSeverityAlerts = fraudAlertRepository.countAlertsBySeverity(FraudAlert.AlertSeverity.HIGH);
            Long criticalAlerts = fraudAlertRepository.countAlertsBySeverity(FraudAlert.AlertSeverity.CRITICAL);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("totalAlerts", totalAlerts);
            response.put("activeAlerts", activeAlerts);
            response.put("highSeverityAlerts", highSeverityAlerts);
            response.put("criticalAlerts", criticalAlerts);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get fraud statistics: " + e.getMessage()));
        }
    }
}
