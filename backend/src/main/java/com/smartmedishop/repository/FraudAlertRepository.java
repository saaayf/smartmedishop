package com.smartmedishop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmedishop.entity.FraudAlert;
import com.smartmedishop.entity.Transaction;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    
    // Find all fraud alerts with transaction and user eagerly loaded
    @Query("SELECT fa FROM FraudAlert fa JOIN FETCH fa.transaction t JOIN FETCH t.user ORDER BY fa.createdAt DESC")
    List<FraudAlert> findAllWithTransactionAndUser();
    
    // Find alerts by transaction
    List<FraudAlert> findByTransaction(Transaction transaction);
    
    // Find alerts by transaction ID
    List<FraudAlert> findByTransactionId(Long transactionId);
    
    // Find alerts by status
    List<FraudAlert> findByStatus(FraudAlert.AlertStatus status);
    
    // Find alerts by severity
    List<FraudAlert> findBySeverity(FraudAlert.AlertSeverity severity);
    
    // Find alerts by date range
    List<FraudAlert> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find high severity alerts
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.severity IN ('HIGH', 'CRITICAL')")
    List<FraudAlert> findHighSeverityAlerts();
    
    // Find unresolved alerts
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.status IN ('ACTIVE', 'INVESTIGATING')")
    List<FraudAlert> findUnresolvedAlerts();
    
    // Find alerts by fraud score threshold
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.fraudScore > :threshold")
    List<FraudAlert> findAlertsByFraudScoreThreshold(@Param("threshold") Double threshold);
    
    // Count alerts by status
    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE fa.status = :status")
    Long countAlertsByStatus(@Param("status") FraudAlert.AlertStatus status);
    
    // Count alerts by severity
    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE fa.severity = :severity")
    Long countAlertsBySeverity(@Param("severity") FraudAlert.AlertSeverity severity);
    
    // Find recent alerts
    @Query("SELECT fa FROM FraudAlert fa ORDER BY fa.createdAt DESC")
    List<FraudAlert> findRecentAlerts();
    
    // Find alerts by user
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.transaction.user.id = :userId")
    List<FraudAlert> findAlertsByUser(@Param("userId") Long userId);
    
    // Find alerts by alert type
    List<FraudAlert> findByAlertType(String alertType);
    
    // Find alerts by resolved by
    List<FraudAlert> findByResolvedBy(String resolvedBy);
    
    // Find alerts created after date
    List<FraudAlert> findByCreatedAtAfter(LocalDateTime date);
    
    // Find alerts resolved after date
    List<FraudAlert> findByResolvedAtAfter(LocalDateTime date);
    
    // Get alert statistics
    @Query("SELECT fa.severity, COUNT(fa) FROM FraudAlert fa GROUP BY fa.severity")
    List<Object[]> getAlertStatisticsBySeverity();
    
    // Get alert statistics by status
    @Query("SELECT fa.status, COUNT(fa) FROM FraudAlert fa GROUP BY fa.status")
    List<Object[]> getAlertStatisticsByStatus();
    
    // Find alerts with high fraud score
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.fraudScore > 0.7")
    List<FraudAlert> findHighFraudScoreAlerts();
    
    // Find alerts by date range and severity
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.createdAt BETWEEN :startDate AND :endDate AND fa.severity = :severity")
    List<FraudAlert> findAlertsByDateRangeAndSeverity(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate, 
                                                      @Param("severity") FraudAlert.AlertSeverity severity);
}
