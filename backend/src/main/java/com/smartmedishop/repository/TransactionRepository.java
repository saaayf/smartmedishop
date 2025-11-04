package com.smartmedishop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmedishop.entity.Transaction;
import com.smartmedishop.entity.User;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transactions by user
    List<Transaction> findByUser(User user);
    
    // Find transactions by user ID
    List<Transaction> findByUserId(Long userId);
    
    // Find transactions by status
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    // Find transactions by risk level
    List<Transaction> findByRiskLevel(Transaction.RiskLevel riskLevel);
    
    // Find fraudulent transactions
    List<Transaction> findByIsFraudTrue();
    
    // Find transactions by amount range
    List<Transaction> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    
    // Find transactions by date range
    List<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find transactions by payment method
    List<Transaction> findByPaymentMethod(String paymentMethod);
    
    // Find transactions by device type
    List<Transaction> findByDeviceType(String deviceType);
    
    // Find transactions by location
    List<Transaction> findByLocationCountry(String country);
    
    // Find high-risk transactions
    @Query("SELECT t FROM Transaction t WHERE t.riskLevel IN ('HIGH', 'CRITICAL')")
    List<Transaction> findHighRiskTransactions();
    
    // Find transactions with fraud score above threshold
    @Query("SELECT t FROM Transaction t WHERE t.fraudScore > :threshold")
    List<Transaction> findTransactionsWithHighFraudScore(@Param("threshold") Double threshold);
    
    // Find recent transactions for user
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactionsByUser(@Param("userId") Long userId);
    
    // Count transactions by user
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId")
    Long countTransactionsByUser(@Param("userId") Long userId);
    
    // Calculate total amount by user
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId")
    BigDecimal getTotalAmountByUser(@Param("userId") Long userId);
    
    // Calculate average amount by user
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.user.id = :userId")
    Double getAverageAmountByUser(@Param("userId") Long userId);
    
    // Find transactions by user and date range
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findTransactionsByUserAndDateRange(@Param("userId") Long userId, 
                                                        @Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    // Find transactions by user and amount
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.amount >= :amount")
    List<Transaction> findTransactionsByUserAndAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    // Find suspicious transactions
    @Query("SELECT t FROM Transaction t WHERE t.fraudScore > 0.5 OR t.riskLevel IN ('HIGH', 'CRITICAL')")
    List<Transaction> findSuspiciousTransactions();
    
    // Find transactions by merchant
    List<Transaction> findByMerchantName(String merchantName);
    
    // Find transactions by transaction type
    List<Transaction> findByTransactionType(String transactionType);
    
    // Get transaction statistics
    @Query("SELECT COUNT(t), AVG(t.amount), MAX(t.amount), MIN(t.amount) FROM Transaction t WHERE t.user.id = :userId")
    Object[] getTransactionStatisticsByUser(@Param("userId") Long userId);
}
