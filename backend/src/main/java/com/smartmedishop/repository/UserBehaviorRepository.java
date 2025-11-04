package com.smartmedishop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmedishop.entity.UserBehavior;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
    
    // Find behavior by user ID
    Optional<UserBehavior> findByUserId(Long userId);
    
    // Find behaviors by preferred payment method
    List<UserBehavior> findByPreferredPaymentMethod(String paymentMethod);
    
    // Find behaviors by device type
    List<UserBehavior> findByPreferredDeviceType(String deviceType);
    
    // Find behaviors by location
    List<UserBehavior> findByLocationCountry(String country);
    
    // Find behaviors with high transaction velocity
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.transactionVelocity > :threshold")
    List<UserBehavior> findBehaviorsWithHighTransactionVelocity(@Param("threshold") Integer threshold);
    
    // Find behaviors with high amount velocity
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.amountVelocity > :threshold")
    List<UserBehavior> findBehaviorsWithHighAmountVelocity(@Param("threshold") Double threshold);
    
    // Find behaviors with unusual patterns
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.unusualPatternsCount > 0")
    List<UserBehavior> findBehaviorsWithUnusualPatterns();
    
    // Find behaviors by last transaction date
    List<UserBehavior> findByLastTransactionDateAfter(LocalDateTime date);
    
    // Find behaviors by transaction frequency
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.transactionFrequencyPerDay > :threshold")
    List<UserBehavior> findBehaviorsByTransactionFrequency(@Param("threshold") Double threshold);
    
    // Find behaviors by weekend transaction ratio
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.weekendTransactionRatio > :threshold")
    List<UserBehavior> findBehaviorsByWeekendTransactionRatio(@Param("threshold") Double threshold);
    
    // Find behaviors by night transaction ratio
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.nightTransactionRatio > :threshold")
    List<UserBehavior> findBehaviorsByNightTransactionRatio(@Param("threshold") Double threshold);
    
    // Find behaviors updated after date
    List<UserBehavior> findByLastUpdatedAfter(LocalDateTime date);
    
    // Find behaviors by average transaction amount range
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.averageTransactionAmount BETWEEN :minAmount AND :maxAmount")
    List<UserBehavior> findBehaviorsByAverageTransactionAmount(@Param("minAmount") Double minAmount, 
                                                               @Param("maxAmount") Double maxAmount);
    
    // Get behavior statistics
    @Query("SELECT AVG(ub.transactionVelocity), AVG(ub.amountVelocity), AVG(ub.averageTransactionAmount) FROM UserBehavior ub")
    Object[] getBehaviorStatistics();
    
    // Find behaviors by user type - REMOVED (no longer possible with userId only)
    
    // Find behaviors with high risk patterns
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.unusualPatternsCount > 3 OR ub.transactionVelocity > 10")
    List<UserBehavior> findBehaviorsWithHighRiskPatterns();
    
    // Find behaviors by location and payment method
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.locationCountry = :country AND ub.preferredPaymentMethod = :paymentMethod")
    List<UserBehavior> findBehaviorsByLocationAndPaymentMethod(@Param("country") String country, 
                                                               @Param("paymentMethod") String paymentMethod);
    
    // Find behaviors by device type and location
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.preferredDeviceType = :deviceType AND ub.locationCountry = :country")
    List<UserBehavior> findBehaviorsByDeviceTypeAndLocation(@Param("deviceType") String deviceType, 
                                                           @Param("country") String country);
    
    // Get behavior patterns by location
    @Query("SELECT ub.locationCountry, COUNT(ub), AVG(ub.transactionVelocity) FROM UserBehavior ub GROUP BY ub.locationCountry")
    List<Object[]> getBehaviorPatternsByLocation();
    
    // Get behavior patterns by payment method
    @Query("SELECT ub.preferredPaymentMethod, COUNT(ub), AVG(ub.amountVelocity) FROM UserBehavior ub GROUP BY ub.preferredPaymentMethod")
    List<Object[]> getBehaviorPatternsByPaymentMethod();
}
