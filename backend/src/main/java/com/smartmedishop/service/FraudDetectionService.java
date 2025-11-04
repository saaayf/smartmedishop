package com.smartmedishop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.smartmedishop.entity.Transaction;
import com.smartmedishop.entity.User;
import com.smartmedishop.entity.UserBehavior;
import com.smartmedishop.repository.TransactionRepository;
import com.smartmedishop.repository.UserBehaviorRepository;

@Service
public class FraudDetectionService {
    
    @Autowired
    private UserBehaviorRepository userBehaviorRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ai.model.base-url:http://localhost:5000}")
    private String aiModelBaseUrl;
    
    @Value("${ai.model.timeout:30000}")
    private int aiModelTimeout;
    
    // Fraud detection result class
    public static class FraudDetectionResult {
        private Double fraudScore;
        private Transaction.RiskLevel riskLevel;
        private Boolean isFraud;
        private List<String> fraudReasons;
        private Map<String, Object> mlExplanation;
        private Map<String, Object> ruleExplanation;
        private Map<String, Object> conclusion;
        
        public FraudDetectionResult() {
            this.fraudReasons = new ArrayList<>();
        }
        
        public FraudDetectionResult(Double fraudScore, Transaction.RiskLevel riskLevel, Boolean isFraud, List<String> fraudReasons) {
            this.fraudScore = fraudScore;
            this.riskLevel = riskLevel;
            this.isFraud = isFraud;
            this.fraudReasons = fraudReasons;
        }
        
        // Getters and setters
        public Double getFraudScore() { return fraudScore; }
        public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }
        
        public Transaction.RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(Transaction.RiskLevel riskLevel) { this.riskLevel = riskLevel; }
        
        public Boolean getIsFraud() { return isFraud; }
        public void setIsFraud(Boolean isFraud) { this.isFraud = isFraud; }
        
        public List<String> getFraudReasons() { return fraudReasons; }
        public void setFraudReasons(List<String> fraudReasons) { this.fraudReasons = fraudReasons; }
        
        public Map<String, Object> getMlExplanation() { return mlExplanation; }
        public void setMlExplanation(Map<String, Object> mlExplanation) { this.mlExplanation = mlExplanation; }
        
        public Map<String, Object> getRuleExplanation() { return ruleExplanation; }
        public void setRuleExplanation(Map<String, Object> ruleExplanation) { this.ruleExplanation = ruleExplanation; }
        
        public Map<String, Object> getConclusion() { return conclusion; }
        public void setConclusion(Map<String, Object> conclusion) { this.conclusion = conclusion; }
    }
    
    // Main fraud detection method
    public FraudDetectionResult analyzeTransaction(Transaction transaction) {
        try {
            // Prepare transaction data for AI model
            Map<String, Object> transactionData = prepareTransactionData(transaction);
            
            // Call AI model API
            FraudDetectionResult result = callAiModel(transactionData);
            
            // Add user-specific fraud reasons
            addUserSpecificFraudReasons(transaction, result);
            
            return result;
            
        } catch (Exception e) {
            // Fallback to basic fraud detection
            return performBasicFraudDetection(transaction);
        }
    }
    
    // Prepare transaction data for AI model
    private Map<String, Object> prepareTransactionData(Transaction transaction) {
        Map<String, Object> data = new HashMap<>();
        
        // Basic transaction data
        data.put("amount", transaction.getAmount().doubleValue());
        data.put("payment_method", transaction.getPaymentMethod());
        data.put("device_type", transaction.getDeviceType());
        data.put("location_country", transaction.getLocationCountry());
        data.put("merchant_name", transaction.getMerchantName());
        data.put("transaction_type", transaction.getTransactionType());
        
        // Time-based features
        LocalDateTime transactionDate = transaction.getTransactionDate();
        data.put("hour", transactionDate.getHour());
        data.put("day_of_week", transactionDate.getDayOfWeek().getValue());
        data.put("month", transactionDate.getMonthValue());
        
        // User-specific features
        User user = transaction.getUser();
        if (user != null) {
            data.put("user_id", user.getId());
            // Calculate account age in days (for user registration checks)
            data.put("user_account_age_days", calculateUserAccountAgeDays(user));
            // Calculate actual age in years (for underage checks)
            Integer actualAge = user.getAge(); // Uses the getAge() method from User entity
            data.put("user_age", actualAge != null ? actualAge : 30); // Default to 30 if not set
            data.put("user_fraud_count", user.getFraudCount());
            
            // IMPORTANT: Get ACTUAL transaction count from database, not cached user stats
            // The current transaction should already be saved by TransactionService before fraud detection runs
            // So we need to include it in our count to get accurate statistics
            List<Transaction> allUserTransactions = transactionRepository.findByUserId(user.getId());
            
            // Ensure we include the current transaction if it's not in the list yet (transaction boundary issue)
            Long currentTransactionId = transaction.getId();
            boolean currentTransactionInList = currentTransactionId != null && 
                allUserTransactions.stream().anyMatch(t -> t.getId().equals(currentTransactionId));
            
            // If current transaction is not in the list, manually include it in our calculations
            int actualTransactionCount = currentTransactionInList ? 
                allUserTransactions.size() : 
                allUserTransactions.size() + 1;
            
            // Calculate total amount - include current transaction amount
            double totalAmount = allUserTransactions.stream()
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
            
            // If current transaction wasn't in the list, add its amount
            if (!currentTransactionInList) {
                totalAmount += transaction.getAmount().doubleValue();
            }
            
            // Log for debugging
            System.out.println("🔍 Fraud Detection Debug:");
            System.out.println("   - Current Transaction ID: " + currentTransactionId);
            System.out.println("   - Transactions in DB: " + allUserTransactions.size());
            System.out.println("   - Current in list: " + currentTransactionInList);
            System.out.println("   - Actual Transaction Count: " + actualTransactionCount);
            System.out.println("   - Total Amount: " + totalAmount);
            
            data.put("user_total_transactions", actualTransactionCount);
            
            // Calculate actual average amount from all transactions (including current)
            if (actualTransactionCount > 0) {
                double actualAverageAmount = totalAmount / actualTransactionCount;
                data.put("user_average_amount", actualAverageAmount);
                System.out.println("   - Average Amount: " + actualAverageAmount);
            } else {
                // No transactions yet (this is the first one)
                data.put("user_average_amount", 0.0);
                System.out.println("   - This is the user's FIRST transaction");
            }
            
            data.put("user_risk_profile", user.getRiskProfile().name());
            
            // Get user behavior data
            Optional<UserBehavior> userBehaviorOpt = userBehaviorRepository.findByUserId(user.getId());
            if (userBehaviorOpt.isPresent()) {
                UserBehavior userBehavior = userBehaviorOpt.get();
                data.put("user_transaction_velocity", userBehavior.getTransactionVelocity());
                data.put("user_amount_velocity", userBehavior.getAmountVelocity());
                data.put("user_average_transaction_amount", userBehavior.getAverageTransactionAmount());
                data.put("user_max_transaction_amount", userBehavior.getMaxTransactionAmount());
                data.put("user_unusual_patterns_count", userBehavior.getUnusualPatternsCount());
                data.put("user_transaction_frequency_per_day", userBehavior.getTransactionFrequencyPerDay());
                data.put("user_weekend_transaction_ratio", userBehavior.getWeekendTransactionRatio());
                data.put("user_night_transaction_ratio", userBehavior.getNightTransactionRatio());
            }
        }
        
        // Debug: Log key values being sent to AI model BEFORE sending
        System.out.println("📤 ===== SENDING TO AI MODEL =====");
        System.out.println("   - user_total_transactions: " + data.get("user_total_transactions"));
        System.out.println("   - user_average_amount: " + data.get("user_average_amount"));
        System.out.println("   - user_max_transaction_amount: " + data.get("user_max_transaction_amount"));
        System.out.println("   - current transaction amount: " + data.get("amount"));
        System.out.println("   - user_account_age_days: " + data.get("user_account_age_days"));
        System.out.println("   - hour: " + data.get("hour"));
        
        // IMPORTANT: Verify the values match expectations
        Integer totalTransactions = (Integer) data.get("user_total_transactions");
        Double averageAmount = (Double) data.get("user_average_amount");
        if (totalTransactions != null && totalTransactions >= 5) {
            System.out.println("   ✅ User has " + totalTransactions + " transactions - 'limited history' SHOULD NOT trigger");
        } else {
            System.out.println("   ⚠️ WARNING: User has " + totalTransactions + " transactions - 'limited history' WILL trigger incorrectly!");
        }
        if (averageAmount != null && averageAmount > 0) {
            System.out.println("   ✅ User has average amount: " + averageAmount + " - 'new user' check SHOULD use history");
        } else {
            System.out.println("   ⚠️ WARNING: User average amount is 0 - 'new user' check WILL use absolute thresholds incorrectly!");
        }
        System.out.println("=====================================");
        
        return data;
    }
    
    // Call AI model API
    private FraudDetectionResult callAiModel(Map<String, Object> transactionData) {
        try {
            // Prepare request - Python API expects data directly
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Convert Integer/Double values to proper types for JSON serialization
            // This ensures Python receives the correct numeric types
            Map<String, Object> requestData = new HashMap<>();
            for (Map.Entry<String, Object> entry : transactionData.entrySet()) {
                Object value = entry.getValue();
                // Convert Integer to int, Double to double for proper JSON serialization
                if (value instanceof Integer) {
                    requestData.put(entry.getKey(), ((Integer) value).intValue());
                } else if (value instanceof Double) {
                    requestData.put(entry.getKey(), ((Double) value).doubleValue());
                } else if (value instanceof Long) {
                    requestData.put(entry.getKey(), ((Long) value).longValue());
                } else {
                    requestData.put(entry.getKey(), value);
                }
            }
            
            // Debug: Log what we're actually sending
            System.out.println("📦 Request Body being sent to AI Model:");
            System.out.println("   - user_total_transactions: " + requestData.get("user_total_transactions") + " (type: " + (requestData.get("user_total_transactions") != null ? requestData.get("user_total_transactions").getClass().getSimpleName() : "null") + ")");
            System.out.println("   - user_average_amount: " + requestData.get("user_average_amount") + " (type: " + (requestData.get("user_average_amount") != null ? requestData.get("user_average_amount").getClass().getSimpleName() : "null") + ")");
            System.out.println("   - user_max_transaction_amount: " + requestData.get("user_max_transaction_amount") + " (type: " + (requestData.get("user_max_transaction_amount") != null ? requestData.get("user_max_transaction_amount").getClass().getSimpleName() : "null") + ")");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
            
            // Call AI model
            String url = aiModelBaseUrl + "/api/analyze-transaction";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Parse response - Python API returns data in 'analysis' object
                Map<String, Object> analysis = (Map<String, Object>) responseBody.get("analysis");
                if (analysis != null) {
                    Double fraudScore = (Double) analysis.get("fraud_score");
                    String riskLevelStr = (String) analysis.get("risk_level");
                    Boolean isFraud = (Boolean) analysis.get("is_fraud");
                    List<String> fraudReasons = (List<String>) analysis.get("reasons");
                    
                    Transaction.RiskLevel riskLevel = Transaction.RiskLevel.valueOf(riskLevelStr);
                    
                    FraudDetectionResult result = new FraudDetectionResult(fraudScore, riskLevel, isFraud, fraudReasons);
                    
                    // Extract explanations for analysts (if available)
                    Map<String, Object> mlExplanation = (Map<String, Object>) analysis.get("ml_explanation");
                    Map<String, Object> ruleExplanation = (Map<String, Object>) analysis.get("rule_explanation");
                    Map<String, Object> conclusion = (Map<String, Object>) analysis.get("conclusion");
                    
                    if (mlExplanation != null) {
                        result.setMlExplanation(mlExplanation);
                    }
                    if (ruleExplanation != null) {
                        result.setRuleExplanation(ruleExplanation);
                    }
                    if (conclusion != null) {
                        result.setConclusion(conclusion);
                    }
                    
                    return result;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error calling AI model: " + e.getMessage());
        }
        
        // Fallback to basic detection
        return performBasicFraudDetection(null);
    }
    
    // Basic fraud detection (fallback)
    private FraudDetectionResult performBasicFraudDetection(Transaction transaction) {
        List<String> fraudReasons = new ArrayList<>();
        Double fraudScore = 0.0;
        
        if (transaction != null) {
            // Check amount
            if (transaction.getAmount().doubleValue() > 10000) {
                fraudScore += 0.3;
                fraudReasons.add("High transaction amount");
            }
            
            // Check time (night transactions)
            int hour = transaction.getTransactionDate().getHour();
            if (hour < 6 || hour > 22) {
                fraudScore += 0.2;
                fraudReasons.add("Transaction made during unusual hours");
            }
            
            // Check user fraud history
            User user = transaction.getUser();
            if (user != null && user.getFraudCount() > 0) {
                fraudScore += 0.2;
                fraudReasons.add("User has fraud history");
            }
        }
        
        // Determine risk level
        Transaction.RiskLevel riskLevel;
        Boolean isFraud;
        
        if (fraudScore >= 0.7) {
            riskLevel = Transaction.RiskLevel.CRITICAL;
            isFraud = true;
        } else if (fraudScore >= 0.5) {
            riskLevel = Transaction.RiskLevel.HIGH;
            isFraud = true;
        } else if (fraudScore >= 0.3) {
            riskLevel = Transaction.RiskLevel.MEDIUM;
            isFraud = false;
        } else {
            riskLevel = Transaction.RiskLevel.LOW;
            isFraud = false;
        }
        
        return new FraudDetectionResult(fraudScore, riskLevel, isFraud, fraudReasons);
    }
    
    // Add user-specific fraud reasons
    private void addUserSpecificFraudReasons(Transaction transaction, FraudDetectionResult result) {
        User user = transaction.getUser();
        if (user == null) return;
        
        // Check user behavior
        Optional<UserBehavior> userBehaviorOpt = userBehaviorRepository.findByUserId(user.getId());
        if (userBehaviorOpt.isPresent()) {
            UserBehavior userBehavior = userBehaviorOpt.get();
            
            // Check payment method consistency
            if (userBehavior.getPreferredPaymentMethod() != null && 
                !userBehavior.getPreferredPaymentMethod().equals(transaction.getPaymentMethod())) {
                result.getFraudReasons().add("Different payment method from usual");
            }
            
            // Check device consistency
            if (userBehavior.getPreferredDeviceType() != null && 
                !userBehavior.getPreferredDeviceType().equals(transaction.getDeviceType())) {
                result.getFraudReasons().add("Different device type from usual");
            }
            
            // Check location consistency
            if (userBehavior.getLocationCountry() != null && 
                !userBehavior.getLocationCountry().equals(transaction.getLocationCountry())) {
                result.getFraudReasons().add("Different location from usual");
            }
            
            // Check amount consistency
            if (transaction.getAmount().doubleValue() > userBehavior.getMaxTransactionAmount()) {
                result.getFraudReasons().add("Amount exceeds user's maximum transaction amount");
            }
        }
    }
    
    // Calculate user account age in days (days since registration)
    private Integer calculateUserAccountAgeDays(User user) {
        if (user.getRegistrationDate() == null) {
            return 0;
        }
        return (int) java.time.Duration.between(user.getRegistrationDate(), LocalDateTime.now()).toDays();
    }
    
    // Update user behavior after transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {org.springframework.dao.DataIntegrityViolationException.class})
    public void updateUserBehavior(Transaction transaction) {
        System.out.println("🔍 updateUserBehavior called for transaction: " + transaction.getId());
        User user = transaction.getUser();
        if (user == null) {
            System.out.println("❌ User is null, skipping behavior update");
            return;
        }
        
        System.out.println("👤 Looking for user behavior for user ID: " + user.getId());
        Optional<UserBehavior> userBehaviorOpt = userBehaviorRepository.findByUserId(user.getId());
        UserBehavior userBehavior;
        
        if (userBehaviorOpt.isPresent()) {
            System.out.println("✅ Found existing user behavior, updating...");
            userBehavior = userBehaviorOpt.get();
        } else {
            System.out.println("🆕 No user behavior found, creating new one...");
            userBehavior = new UserBehavior(user);
            userBehavior.setTransactionVelocity(0);
            userBehavior.setAmountVelocity(0.0);
            userBehavior.setAverageTransactionAmount(0.0);
            userBehavior.setMaxTransactionAmount(0.0);
            userBehavior.setUnusualPatternsCount(0);
            userBehavior.setTransactionFrequencyPerDay(0.0);
            userBehavior.setWeekendTransactionRatio(0.0);
            userBehavior.setNightTransactionRatio(0.0);
        }
        
        // Update transaction velocity
        userBehavior.setTransactionVelocity(userBehavior.getTransactionVelocity() + 1);
        
        // Update amount velocity
        userBehavior.setAmountVelocity(userBehavior.getAmountVelocity() + transaction.getAmount().doubleValue());
        
        // Update average transaction amount
        Double currentAverage = userBehavior.getAverageTransactionAmount();
        Integer totalTransactions = userBehavior.getTransactionVelocity();
        Double newAverage = ((currentAverage * (totalTransactions - 1)) + transaction.getAmount().doubleValue()) / totalTransactions;
        userBehavior.setAverageTransactionAmount(newAverage);
        
        // Update max transaction amount
        if (transaction.getAmount().doubleValue() > userBehavior.getMaxTransactionAmount()) {
            userBehavior.setMaxTransactionAmount(transaction.getAmount().doubleValue());
        }
        
        // Update min transaction amount
        if (userBehavior.getMinTransactionAmount() == null || userBehavior.getMinTransactionAmount() == 0.0 || 
            transaction.getAmount().doubleValue() < userBehavior.getMinTransactionAmount()) {
            userBehavior.setMinTransactionAmount(transaction.getAmount().doubleValue());
            System.out.println("💰 Updated min transaction amount: " + transaction.getAmount().doubleValue());
        }
        
        // Update last transaction date
        userBehavior.setLastTransactionDate(transaction.getTransactionDate());
        
        // Update transaction-specific behavior patterns based on MOST COMMON values from ALL transactions
        // This ensures "preferred" attributes reflect the user's actual patterns, not just the last transaction
        List<Transaction> allUserTransactions = transactionRepository.findByUserId(user.getId());
        
        // Calculate most common location country
        if (!allUserTransactions.isEmpty()) {
            Map<String, Integer> countryCounts = new HashMap<>();
            for (Transaction t : allUserTransactions) {
                if (t.getLocationCountry() != null && !t.getLocationCountry().isEmpty()) {
                    countryCounts.put(t.getLocationCountry(), countryCounts.getOrDefault(t.getLocationCountry(), 0) + 1);
                }
            }
            String mostCommonCountry = countryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            if (mostCommonCountry != null) {
                userBehavior.setLocationCountry(mostCommonCountry);
                System.out.println("🌍 Updated location country (most common): " + mostCommonCountry);
            }
            
            // Calculate most common payment method
            Map<String, Integer> paymentMethodCounts = new HashMap<>();
            for (Transaction t : allUserTransactions) {
                if (t.getPaymentMethod() != null && !t.getPaymentMethod().isEmpty()) {
                    paymentMethodCounts.put(t.getPaymentMethod(), paymentMethodCounts.getOrDefault(t.getPaymentMethod(), 0) + 1);
                }
            }
            String mostCommonPaymentMethod = paymentMethodCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            if (mostCommonPaymentMethod != null) {
                userBehavior.setPreferredPaymentMethod(mostCommonPaymentMethod);
                System.out.println("💳 Updated preferred payment method (most common): " + mostCommonPaymentMethod);
            }
            
            // Calculate most common device type
            Map<String, Integer> deviceTypeCounts = new HashMap<>();
            for (Transaction t : allUserTransactions) {
                if (t.getDeviceType() != null && !t.getDeviceType().isEmpty()) {
                    deviceTypeCounts.put(t.getDeviceType(), deviceTypeCounts.getOrDefault(t.getDeviceType(), 0) + 1);
                }
            }
            String mostCommonDeviceType = deviceTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            if (mostCommonDeviceType != null) {
                userBehavior.setPreferredDeviceType(mostCommonDeviceType);
                System.out.println("📱 Updated preferred device type (most common): " + mostCommonDeviceType);
            }
        }
        
        // Update transaction frequency per day
        userBehavior.setTransactionFrequencyPerDay(calculateTransactionFrequency(userBehavior));
        
        // Update weekend transaction ratio
        userBehavior.setWeekendTransactionRatio(calculateWeekendTransactionRatio(userBehavior));
        
        // Update night transaction ratio
        userBehavior.setNightTransactionRatio(calculateNightTransactionRatio(userBehavior));
        
        // Save user behavior with error handling
        try {
            userBehaviorRepository.save(userBehavior);
            System.out.println("💾 User behavior updated successfully!");
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("❌ Error saving user behavior: " + e.getMessage());
            // Don't rethrow the exception to prevent transaction failure
        }
    }
    
    // Calculate transaction frequency per day
    private Double calculateTransactionFrequency(UserBehavior userBehavior) {
        if (userBehavior.getLastTransactionDate() == null) {
            return 0.0;
        }
        
        long daysBetween = java.time.Duration.between(userBehavior.getCreatedAt(), userBehavior.getLastTransactionDate()).toDays();
        if (daysBetween == 0) {
            return (double) userBehavior.getTransactionVelocity();
        }
        
        return (double) userBehavior.getTransactionVelocity() / daysBetween;
    }
    
    // Calculate weekend transaction ratio
    private Double calculateWeekendTransactionRatio(UserBehavior userBehavior) {
        // This would require additional data tracking
        // For now, return a placeholder value
        return 0.0;
    }
    
    // Calculate night transaction ratio
    private Double calculateNightTransactionRatio(UserBehavior userBehavior) {
        // This would require additional data tracking
        // For now, return a placeholder value
        return 0.0;
    }
    
}
