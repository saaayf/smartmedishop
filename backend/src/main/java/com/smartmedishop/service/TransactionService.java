package com.smartmedishop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmedishop.entity.FraudAlert;
import com.smartmedishop.entity.Transaction;
import com.smartmedishop.entity.User;
import com.smartmedishop.repository.FraudAlertRepository;
import com.smartmedishop.repository.TransactionRepository;
import com.smartmedishop.repository.UserRepository;

@Service
@Transactional
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private FraudAlertRepository fraudAlertRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FraudDetectionService fraudDetectionService;
    
    @Autowired
    private com.smartmedishop.service.StockService stockService;
    
    // Transaction CRUD operations
    public Transaction createTransaction(Transaction transaction) {
        // Set transaction date if not set
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }
        
        // Set status if not set
        if (transaction.getStatus() == null) {
            transaction.setStatus(Transaction.TransactionStatus.PENDING);
        }
        
        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Perform fraud detection
        performFraudDetection(savedTransaction);
        
        return savedTransaction;
    }
    
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }
    
    public List<Transaction> findByUser(User user) {
        return transactionRepository.findByUser(user);
    }
    
    public List<Transaction> findByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
    }
    
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }
    
    public List<Transaction> findByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }
    
    public List<Transaction> findByRiskLevel(Transaction.RiskLevel riskLevel) {
        return transactionRepository.findByRiskLevel(riskLevel);
    }
    
    public List<Transaction> findFraudulentTransactions() {
        return transactionRepository.findByIsFraudTrue();
    }
    
    public List<Transaction> findHighRiskTransactions() {
        return transactionRepository.findHighRiskTransactions();
    }
    
    public List<Transaction> findSuspiciousTransactions() {
        return transactionRepository.findSuspiciousTransactions();
    }
    
    public List<Transaction> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findByAmountBetween(minAmount, maxAmount);
    }
    
    public List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }
    
    public List<Transaction> findByPaymentMethod(String paymentMethod) {
        return transactionRepository.findByPaymentMethod(paymentMethod);
    }
    
    public List<Transaction> findByDeviceType(String deviceType) {
        return transactionRepository.findByDeviceType(deviceType);
    }
    
    public List<Transaction> findByLocation(String country) {
        return transactionRepository.findByLocationCountry(country);
    }
    
    public List<Transaction> findRecentTransactionsByUser(Long userId) {
        return transactionRepository.findRecentTransactionsByUser(userId);
    }
    
    public List<Transaction> findByUserAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findTransactionsByUserAndDateRange(userId, startDate, endDate);
    }
    
    public List<Transaction> findByUserAndAmount(Long userId, BigDecimal amount) {
        return transactionRepository.findTransactionsByUserAndAmount(userId, amount);
    }
    
    public List<Transaction> findByMerchant(String merchantName) {
        return transactionRepository.findByMerchantName(merchantName);
    }
    
    public List<Transaction> findByTransactionType(String transactionType) {
        return transactionRepository.findByTransactionType(transactionType);
    }
    
    public Transaction updateTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
    
    public boolean deleteTransaction(Long id) {
        try {
            transactionRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            return false;
        }
    }
    
    // Transaction statistics
    public Long countTransactionsByUser(Long userId) {
        return transactionRepository.countTransactionsByUser(userId);
    }
    
    public BigDecimal getTotalAmountByUser(Long userId) {
        return transactionRepository.getTotalAmountByUser(userId);
    }
    
    public Double getAverageAmountByUser(Long userId) {
        return transactionRepository.getAverageAmountByUser(userId);
    }
    
    public Object[] getTransactionStatisticsByUser(Long userId) {
        return transactionRepository.getTransactionStatisticsByUser(userId);
    }
    
    // Fraud detection
    private void performFraudDetection(Transaction transaction) {
        try {
            // Call AI fraud detection service
            FraudDetectionService.FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);
            
            // Update transaction with fraud detection results
            transaction.setFraudScore(result.getFraudScore());
            transaction.setRiskLevel(result.getRiskLevel());
            transaction.setIsFraud(result.getIsFraud());
            transaction.setFraudReasons(String.join(", ", result.getFraudReasons()));
            
            // Store explanations as JSON strings
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                if (result.getMlExplanation() != null) {
                    transaction.setMlExplanation(objectMapper.writeValueAsString(result.getMlExplanation()));
                }
                if (result.getRuleExplanation() != null) {
                    transaction.setRuleExplanation(objectMapper.writeValueAsString(result.getRuleExplanation()));
                }
                if (result.getConclusion() != null) {
                    transaction.setConclusion(objectMapper.writeValueAsString(result.getConclusion()));
                }
            } catch (Exception e) {
                System.err.println("Error storing explanations: " + e.getMessage());
            }
            
            // Save updated transaction
            transactionRepository.save(transaction);
            
            // Create fraud alert if fraud detected
            if (result.getIsFraud() && result.getFraudScore() > 0.5) {
                createFraudAlert(transaction, result);
            }
            
            // Update user statistics
            updateUserTransactionStats(transaction);
            
            // Update user behavior patterns
            System.out.println("🔄 Calling updateUserBehavior for transaction: " + transaction.getId());
            fraudDetectionService.updateUserBehavior(transaction);
            
        } catch (Exception e) {
            // Log error and continue with transaction
            System.err.println("Error in fraud detection: " + e.getMessage());
        }
    }
    
    private void createFraudAlert(Transaction transaction, FraudDetectionService.FraudDetectionResult result) {
        FraudAlert alert = new FraudAlert(
            transaction,
            "AI_FRAUD_DETECTION",
            FraudAlert.AlertSeverity.valueOf(result.getRiskLevel().name()),
            "AI model detected potential fraud: " + String.join(", ", result.getFraudReasons())
        );
        
        alert.setFraudScore(result.getFraudScore());
        alert.setRiskFactors(String.join(", ", result.getFraudReasons()));
        
        fraudAlertRepository.save(alert);
    }
    
    private void updateUserTransactionStats(Transaction transaction) {
        User user = transaction.getUser();
        if (user != null) {
            // Update user transaction count and average amount
            user.setTotalTransactions(user.getTotalTransactions() + 1);
            
            // Update average amount
            Double currentAverage = user.getAverageAmount();
            Integer totalTransactions = user.getTotalTransactions();
            Double newAverage = ((currentAverage * (totalTransactions - 1)) + transaction.getAmount().doubleValue()) / totalTransactions;
            user.setAverageAmount(newAverage); // Fixed: was missing setAverageAmount()
            
            // Update fraud count if fraud detected
            if (transaction.getIsFraud()) {
                user.setFraudCount(user.getFraudCount() + 1);
                
                // Update risk profile based on fraud count
                updateUserRiskProfile(user);
            }
            
            userRepository.save(user);
        }
    }
    
    private void updateUserRiskProfile(User user) {
        int fraudCount = user.getFraudCount();
        
        // Escalate risk profile based on fraud count
        if (fraudCount >= 3) {
            user.setRiskProfile(User.RiskProfile.CRITICAL);
        } else if (fraudCount >= 2) {
            user.setRiskProfile(User.RiskProfile.HIGH);
        } else if (fraudCount >= 1) {
            user.setRiskProfile(User.RiskProfile.MEDIUM);
        }
    }
    
    // Transaction processing
    public Transaction processTransaction(Long transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            return transactionRepository.save(transaction);
        }
        return null;
    }
    
    // Recalculate user statistics for existing users
    public void recalculateUserStatistics(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            List<Transaction> userTransactions = transactionRepository.findByUserId(userId);
            
            // Reset counters
            user.setTotalTransactions(0);
            user.setAverageAmount(0.0);
            user.setFraudCount(0);
            
            // Recalculate from transactions
            double totalAmount = 0.0;
            int fraudCount = 0;
            
            for (Transaction transaction : userTransactions) {
                totalAmount += transaction.getAmount().doubleValue();
                if (transaction.getIsFraud()) {
                    fraudCount++;
                }
            }
            
            // Update user statistics
            user.setTotalTransactions(userTransactions.size());
            if (userTransactions.size() > 0) {
                user.setAverageAmount(totalAmount / userTransactions.size());
            }
            user.setFraudCount(fraudCount);
            
            // Update risk profile based on fraud count
            updateUserRiskProfile(user);
            
            userRepository.save(user);
        }
    }
    
    // Recalculate all user statistics
    public void recalculateAllUserStatistics() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            recalculateUserStatistics(user.getId());
        }
    }
    
    
    public Transaction failTransaction(Long transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            return transactionRepository.save(transaction);
        }
        return null;
    }
    
    public Transaction cancelTransaction(Long transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
            return transactionRepository.save(transaction);
        }
        return null;
    }
}
