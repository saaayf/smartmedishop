package com.smartmedishop.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmedishop.entity.Transaction;
import com.smartmedishop.entity.User;
import com.smartmedishop.security.CustomUserDetailsService;
import com.smartmedishop.service.TransactionService;
import com.smartmedishop.service.UserService;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private com.smartmedishop.service.StockService stockService;
    
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> transactionData, 
                                             Authentication authentication) {
        try {
            // Check if authentication is null
            if (authentication == null) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication is null"));
            }
            
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Create transaction entity
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setAmount(new java.math.BigDecimal(transactionData.get("amount").toString()));
            transaction.setPaymentMethod(transactionData.get("paymentMethod") != null ? transactionData.get("paymentMethod").toString() : "credit_card");
            transaction.setDeviceType(transactionData.get("deviceType") != null ? transactionData.get("deviceType").toString() : "desktop");
            transaction.setIpAddress(transactionData.get("ipAddress") != null ? transactionData.get("ipAddress").toString() : "127.0.0.1");
            transaction.setLocationCountry(transactionData.get("locationCountry") != null ? transactionData.get("locationCountry").toString() : "US");
            transaction.setMerchantName(transactionData.get("merchantName") != null ? transactionData.get("merchantName").toString() : "Unknown Merchant");
            transaction.setTransactionType(transactionData.get("transactionType") != null ? transactionData.get("transactionType").toString() : "purchase");
            transaction.setTransactionDate(LocalDateTime.now());
            
            // Save transaction (this will trigger fraud detection)
            Transaction savedTransaction = transactionService.createTransaction(transaction);

            // Handle optional items list to decrement stock
            try {
                if (transactionData.containsKey("items") && transactionData.get("items") instanceof java.util.List) {
                    java.util.List items = (java.util.List) transactionData.get("items");
                    for (Object o : items) {
                        if (o instanceof java.util.Map) {
                            java.util.Map item = (java.util.Map) o;
                            String sku = item.get("sku") != null ? item.get("sku").toString() : null;
                            Integer qty = item.get("quantity") != null ? Integer.parseInt(item.get("quantity").toString()) : 1;
                            if (sku != null) {
                                stockService.recordSaleBySku(sku, qty, "SALE for transaction " + savedTransaction.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Log but don't fail the transaction
                System.err.println("Error decrementing stock: " + e.getMessage());
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", savedTransaction.getId());
            response.put("amount", savedTransaction.getAmount());
            response.put("status", savedTransaction.getStatus());
            response.put("fraudScore", savedTransaction.getFraudScore());
            response.put("riskLevel", savedTransaction.getRiskLevel());
            response.put("isFraud", savedTransaction.getIsFraud());
            response.put("fraudReasons", savedTransaction.getFraudReasons());
            response.put("transactionDate", savedTransaction.getTransactionDate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Transaction creation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/my-transactions")
    public ResponseEntity<?> getMyTransactions(Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Get user transactions
            List<Transaction> transactions = transactionService.findByUser(user);
            
            // Create response
            List<Map<String, Object>> response = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("id", transaction.getId());
                    tx.put("amount", transaction.getAmount());
                    tx.put("paymentMethod", transaction.getPaymentMethod());
                    tx.put("status", transaction.getStatus());
                    tx.put("fraudScore", transaction.getFraudScore());
                    tx.put("riskLevel", transaction.getRiskLevel());
                    tx.put("isFraud", transaction.getIsFraud());
                    tx.put("transactionDate", transaction.getTransactionDate());
                    tx.put("merchantName", transaction.getMerchantName());
                    tx.put("userId", transaction.getUser().getId());
                    tx.put("username", transaction.getUser().getUsername());
                    return tx;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get transactions: " + e.getMessage()));
        }
    }
    
    // Get all transactions (for FRAUD_ANALYST and ADMIN)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getAllTransactions(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "transactionDate") String sortBy,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "desc") String sortDir,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long userId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String riskLevel,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean isFraud) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            List<Transaction> transactions;
            
            // Start with all transactions
            transactions = transactionService.findAll();
            System.out.println("🔍 Total transactions before filtering: " + transactions.size());
            
            // Apply filters (can be combined)
            if (userId != null) {
                System.out.println("🔍 Filtering by userId: " + userId);
                transactions = transactions.stream()
                    .filter(t -> t.getUser().getId().equals(userId))
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("✅ Transactions after userId filter: " + transactions.size());
            }
            
            if (riskLevel != null && !riskLevel.isEmpty()) {
                System.out.println("🔍 Filtering by riskLevel: " + riskLevel);
                Transaction.RiskLevel risk = Transaction.RiskLevel.valueOf(riskLevel.toUpperCase());
                transactions = transactions.stream()
                    .filter(t -> t.getRiskLevel() == risk)
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("✅ Transactions after riskLevel filter: " + transactions.size());
            }
            
            if (isFraud != null) {
                boolean isFraudValue = isFraud;
                System.out.println("🔍 Filtering by isFraud: " + isFraudValue + " (Safe = false, Fraud = true)");
                int beforeFilter = transactions.size();
                
                // Filter: if isFraudValue is false, show only non-fraudulent (isFraud == false or null treated as false)
                //         if isFraudValue is true, show only fraudulent (isFraud == true)
                transactions = transactions.stream()
                    .filter(t -> {
                        Boolean transactionIsFraud = t.getIsFraud();
                        // Handle null as false (safe)
                        boolean actualIsFraud = Boolean.TRUE.equals(transactionIsFraud);
                        return actualIsFraud == isFraudValue;
                    })
                    .collect(java.util.stream.Collectors.toList());
                
                System.out.println("✅ Transactions after isFraud filter: " + beforeFilter + " -> " + transactions.size());
            }
            
            // Save total count before pagination (for pagination info)
            long totalElements = transactions.size();
            
            // Sort and paginate manually (since we're filtering, we'll paginate in-memory)
            transactions = transactions.stream()
                .sorted((a, b) -> {
                    if ("desc".equalsIgnoreCase(sortDir)) {
                        return b.getTransactionDate().compareTo(a.getTransactionDate());
                    } else {
                        return a.getTransactionDate().compareTo(b.getTransactionDate());
                    }
                })
                .skip(page * size)
                .limit(size)
                .toList();
            
            // Create response
            List<Map<String, Object>> responseList = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("id", transaction.getId());
                    tx.put("userId", transaction.getUser().getId());
                    tx.put("username", transaction.getUser().getUsername());
                    tx.put("email", transaction.getUser().getEmail());
                    tx.put("amount", transaction.getAmount());
                    tx.put("paymentMethod", transaction.getPaymentMethod());
                    tx.put("status", transaction.getStatus());
                    tx.put("fraudScore", transaction.getFraudScore());
                    tx.put("riskLevel", transaction.getRiskLevel());
                    tx.put("isFraud", transaction.getIsFraud());
                    tx.put("fraudReasons", transaction.getFraudReasons());
                    tx.put("transactionDate", transaction.getTransactionDate());
                    tx.put("merchantName", transaction.getMerchantName());
                    tx.put("deviceType", transaction.getDeviceType());
                    tx.put("locationCountry", transaction.getLocationCountry());
                    tx.put("transactionType", transaction.getTransactionType());
                    return tx;
                })
                .toList();
            
            // Calculate pagination info based on filtered results
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", responseList);
            response.put("currentPage", page);
            response.put("totalItems", totalElements);
            response.put("totalPages", totalPages);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get all transactions: " + e.getMessage()));
        }
    }
    
    // Get transactions by user ID (for FRAUD_ANALYST and ADMIN)
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getTransactionsByUserId(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionService.findByUserId(userId);
            
            // Create response
            List<Map<String, Object>> response = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("id", transaction.getId());
                    tx.put("amount", transaction.getAmount());
                    tx.put("paymentMethod", transaction.getPaymentMethod());
                    tx.put("status", transaction.getStatus());
                    tx.put("fraudScore", transaction.getFraudScore());
                    tx.put("riskLevel", transaction.getRiskLevel());
                    tx.put("isFraud", transaction.getIsFraud());
                    tx.put("fraudReasons", transaction.getFraudReasons());
                    tx.put("transactionDate", transaction.getTransactionDate());
                    tx.put("merchantName", transaction.getMerchantName());
                    tx.put("deviceType", transaction.getDeviceType());
                    tx.put("locationCountry", transaction.getLocationCountry());
                    tx.put("transactionType", transaction.getTransactionType());
                    return tx;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get transactions: " + e.getMessage()));
        }
    }
    
    // Get all transaction statistics (for FRAUD_ANALYST and ADMIN)
    @GetMapping("/statistics/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getAllTransactionStatistics() {
        try {
            List<Transaction> allTransactions = transactionService.findAll();
            
            // Calculate statistics
            long totalTransactions = allTransactions.size();
            long fraudulentTransactions = allTransactions.stream()
                .filter(Transaction::getIsFraud)
                .count();
            long highRiskTransactions = allTransactions.stream()
                .filter(t -> t.getRiskLevel() == Transaction.RiskLevel.HIGH || 
                           t.getRiskLevel() == Transaction.RiskLevel.CRITICAL)
                .count();
            
            double totalAmount = allTransactions.stream()
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
            double averageAmount = totalTransactions > 0 ? totalAmount / totalTransactions : 0;
            double averageFraudScore = allTransactions.stream()
                .filter(t -> t.getFraudScore() != null)
                .mapToDouble(Transaction::getFraudScore)
                .average()
                .orElse(0.0);
            
            // Risk level distribution
            Map<String, Long> riskLevelDistribution = new HashMap<>();
            riskLevelDistribution.put("LOW", allTransactions.stream()
                .filter(t -> t.getRiskLevel() == Transaction.RiskLevel.LOW)
                .count());
            riskLevelDistribution.put("MEDIUM", allTransactions.stream()
                .filter(t -> t.getRiskLevel() == Transaction.RiskLevel.MEDIUM)
                .count());
            riskLevelDistribution.put("HIGH", allTransactions.stream()
                .filter(t -> t.getRiskLevel() == Transaction.RiskLevel.HIGH)
                .count());
            riskLevelDistribution.put("CRITICAL", allTransactions.stream()
                .filter(t -> t.getRiskLevel() == Transaction.RiskLevel.CRITICAL)
                .count());
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalTransactions", totalTransactions);
            response.put("fraudulentTransactions", fraudulentTransactions);
            response.put("highRiskTransactions", highRiskTransactions);
            response.put("totalAmount", totalAmount);
            response.put("averageAmount", averageAmount);
            response.put("averageFraudScore", averageFraudScore);
            response.put("riskLevelDistribution", riskLevelDistribution);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get all transaction statistics: " + e.getMessage()));
        }
    }
    
    
    @PostMapping("/{id}/process")
    public ResponseEntity<?> processTransaction(@PathVariable Long id, Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Get transaction
            var transactionOpt = transactionService.findById(id);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Transaction transaction = transactionOpt.get();
            
            // Check if user owns this transaction
            if (!transaction.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            // Process transaction
            Transaction processedTransaction = transactionService.processTransaction(id);
            
            if (processedTransaction == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to process transaction"));
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", processedTransaction.getId());
            response.put("status", processedTransaction.getStatus());
            response.put("message", "Transaction processed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to process transaction: " + e.getMessage()));
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<?> getTransactionStatistics(Authentication authentication) {
        try {
            // Get current user
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Get statistics
            Long totalTransactions = transactionService.countTransactionsByUser(user.getId());
            var totalAmount = transactionService.getTotalAmountByUser(user.getId());
            Double averageAmount = transactionService.getAverageAmountByUser(user.getId());
            var statistics = transactionService.getTransactionStatisticsByUser(user.getId());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("totalTransactions", totalTransactions);
            response.put("totalAmount", totalAmount);
            response.put("averageAmount", averageAmount);
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }
    
    @PostMapping("/recalculate-user-stats/{userId}")
    public ResponseEntity<?> recalculateUserStatistics(@PathVariable Long userId) {
        try {
            transactionService.recalculateUserStatistics(userId);
            return ResponseEntity.ok(Map.of("message", "User statistics recalculated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to recalculate user statistics: " + e.getMessage()));
        }
    }
    
    @PostMapping("/recalculate-all-user-stats")
    public ResponseEntity<?> recalculateAllUserStatistics() {
        try {
            transactionService.recalculateAllUserStatistics();
            return ResponseEntity.ok(Map.of("message", "All user statistics recalculated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to recalculate all user statistics: " + e.getMessage()));
        }
    }
    
    // Get single transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id, Authentication authentication) {
        try {
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            var transactionOpt = transactionService.findById(id);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Check if user owns this transaction or is admin/fraud analyst
                if (!transaction.getUser().getId().equals(user.getId()) && 
                    !user.getUserType().equals(User.UserType.ADMIN) && 
                    !user.getUserType().equals(User.UserType.FRAUD_ANALYST)) {
                    return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied"));
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("id", transaction.getId());
                response.put("amount", transaction.getAmount());
                response.put("paymentMethod", transaction.getPaymentMethod());
                response.put("status", transaction.getStatus());
                response.put("fraudScore", transaction.getFraudScore());
                response.put("riskLevel", transaction.getRiskLevel());
                response.put("isFraud", transaction.getIsFraud());
                response.put("fraudReasons", transaction.getFraudReasons());
                response.put("transactionDate", transaction.getTransactionDate());
                response.put("merchantName", transaction.getMerchantName());
                response.put("deviceType", transaction.getDeviceType());
                response.put("locationCountry", transaction.getLocationCountry());
                response.put("transactionType", transaction.getTransactionType());
                response.put("ipAddress", transaction.getIpAddress());
                response.put("createdAt", transaction.getCreatedAt());
                
                // Parse and include explanations if available
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    if (transaction.getMlExplanation() != null && !transaction.getMlExplanation().isEmpty()) {
                        response.put("mlExplanation", objectMapper.readValue(transaction.getMlExplanation(), Map.class));
                    }
                    if (transaction.getRuleExplanation() != null && !transaction.getRuleExplanation().isEmpty()) {
                        response.put("ruleExplanation", objectMapper.readValue(transaction.getRuleExplanation(), Map.class));
                    }
                    if (transaction.getConclusion() != null && !transaction.getConclusion().isEmpty()) {
                        response.put("conclusion", objectMapper.readValue(transaction.getConclusion(), Map.class));
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing explanations: " + e.getMessage());
                }
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get transaction: " + e.getMessage()));
        }
    }
    
    // Update transaction
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, 
                                            @RequestBody Map<String, Object> transactionData,
                                            Authentication authentication) {
        try {
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            var transactionOpt = transactionService.findById(id);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Check if user owns this transaction or is admin/fraud analyst
                if (!transaction.getUser().getId().equals(user.getId()) && 
                    !user.getUserType().equals(User.UserType.ADMIN) && 
                    !user.getUserType().equals(User.UserType.FRAUD_ANALYST)) {
                    return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied"));
                }
                
                // Update transaction fields
                if (transactionData.containsKey("amount")) {
                    transaction.setAmount(new java.math.BigDecimal(transactionData.get("amount").toString()));
                }
                if (transactionData.containsKey("paymentMethod")) {
                    transaction.setPaymentMethod(transactionData.get("paymentMethod").toString());
                }
                if (transactionData.containsKey("merchantName")) {
                    transaction.setMerchantName(transactionData.get("merchantName").toString());
                }
                if (transactionData.containsKey("deviceType")) {
                    transaction.setDeviceType(transactionData.get("deviceType").toString());
                }
                if (transactionData.containsKey("locationCountry")) {
                    transaction.setLocationCountry(transactionData.get("locationCountry").toString());
                }
                if (transactionData.containsKey("transactionType")) {
                    transaction.setTransactionType(transactionData.get("transactionType").toString());
                }
                
                Transaction updatedTransaction = transactionService.updateTransaction(transaction);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Transaction updated successfully");
                response.put("transaction", updatedTransaction);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update transaction: " + e.getMessage()));
        }
    }
    
    // Delete transaction
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, Authentication authentication) {
        try {
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            var transactionOpt = transactionService.findById(id);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Check if user owns this transaction or is admin/fraud analyst
                if (!transaction.getUser().getId().equals(user.getId()) && 
                    !user.getUserType().equals(User.UserType.ADMIN) && 
                    !user.getUserType().equals(User.UserType.FRAUD_ANALYST)) {
                    return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied"));
                }
                
                // Delete transaction
                boolean deleted = transactionService.deleteTransaction(id);
                
                if (deleted) {
                    // Recalculate user statistics after deletion
                    transactionService.recalculateUserStatistics(transaction.getUser().getId());
                    
                    return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
                } else {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to delete transaction"));
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete transaction: " + e.getMessage()));
        }
    }
}
