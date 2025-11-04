package com.smartmedishop.repository;

import com.smartmedishop.entity.User;
import com.smartmedishop.entity.UserRiskScore;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRiskScoreRepository extends JpaRepository<UserRiskScore, Long> {
    
    // Find risk scores by user
    List<UserRiskScore> findByUser(User user);
    
    // Find risk scores by user ID
    List<UserRiskScore> findByUserId(Long userId);
    
    // Find active risk scores
    List<UserRiskScore> findByIsActiveTrue();
    
    // Find risk scores by risk level
    List<UserRiskScore> findByRiskLevel(UserRiskScore.RiskLevel riskLevel);
    
    // Find risk scores by date range
    List<UserRiskScore> findByCalculatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find high risk scores
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.riskScore > :threshold")
    List<UserRiskScore> findHighRiskScores(@Param("threshold") Double threshold);
    
    // Find risk scores by model version
    List<UserRiskScore> findByModelVersion(String modelVersion);
    
    // Find risk scores by confidence score
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.confidenceScore > :threshold")
    List<UserRiskScore> findRiskScoresByConfidence(@Param("threshold") Double threshold);
    
    // Find latest risk score for user
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.user.id = :userId AND urs.isActive = true ORDER BY urs.calculatedAt DESC")
    List<UserRiskScore> findLatestRiskScoreByUser(@Param("userId") Long userId);
    
    // Find risk scores by user and risk level
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.user.id = :userId AND urs.riskLevel = :riskLevel")
    List<UserRiskScore> findRiskScoresByUserAndRiskLevel(@Param("userId") Long userId, 
                                                        @Param("riskLevel") UserRiskScore.RiskLevel riskLevel);
    
    // Find risk scores by user and date range
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.user.id = :userId AND urs.calculatedAt BETWEEN :startDate AND :endDate")
    List<UserRiskScore> findRiskScoresByUserAndDateRange(@Param("userId") Long userId, 
                                                          @Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);
    
    // Count risk scores by risk level
    @Query("SELECT COUNT(urs) FROM UserRiskScore urs WHERE urs.riskLevel = :riskLevel")
    Long countRiskScoresByRiskLevel(@Param("riskLevel") UserRiskScore.RiskLevel riskLevel);
    
    // Count active risk scores by risk level
    @Query("SELECT COUNT(urs) FROM UserRiskScore urs WHERE urs.isActive = true AND urs.riskLevel = :riskLevel")
    Long countActiveRiskScoresByRiskLevel(@Param("riskLevel") UserRiskScore.RiskLevel riskLevel);
    
    // Find risk scores by user type
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.user.userType = :userType")
    List<UserRiskScore> findRiskScoresByUserType(@Param("userType") User.UserType userType);
    
    // Find risk scores by model version and risk level
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.modelVersion = :modelVersion AND urs.riskLevel = :riskLevel")
    List<UserRiskScore> findRiskScoresByModelVersionAndRiskLevel(@Param("modelVersion") String modelVersion, 
                                                                   @Param("riskLevel") UserRiskScore.RiskLevel riskLevel);
    
    // Get risk score statistics
    @Query("SELECT AVG(urs.riskScore), MAX(urs.riskScore), MIN(urs.riskScore) FROM UserRiskScore urs WHERE urs.isActive = true")
    Object[] getRiskScoreStatistics();
    
    // Get risk score statistics by risk level
    @Query("SELECT urs.riskLevel, COUNT(urs), AVG(urs.riskScore) FROM UserRiskScore urs WHERE urs.isActive = true GROUP BY urs.riskLevel")
    List<Object[]> getRiskScoreStatisticsByRiskLevel();
    
    // Find risk scores with high confidence
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.confidenceScore > 0.8")
    List<UserRiskScore> findHighConfidenceRiskScores();
    
    // Find risk scores by user and confidence
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.user.id = :userId AND urs.confidenceScore > :threshold")
    List<UserRiskScore> findRiskScoresByUserAndConfidence(@Param("userId") Long userId, 
                                                           @Param("threshold") Double threshold);
    
    // Find risk scores by date range and risk level
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.calculatedAt BETWEEN :startDate AND :endDate AND urs.riskLevel = :riskLevel")
    List<UserRiskScore> findRiskScoresByDateRangeAndRiskLevel(@Param("startDate") LocalDateTime startDate, 
                                                               @Param("endDate") LocalDateTime endDate, 
                                                               @Param("riskLevel") UserRiskScore.RiskLevel riskLevel);
    
    // Find risk scores by model version and date range
    @Query("SELECT urs FROM UserRiskScore urs WHERE urs.modelVersion = :modelVersion AND urs.calculatedAt BETWEEN :startDate AND :endDate")
    List<UserRiskScore> findRiskScoresByModelVersionAndDateRange(@Param("modelVersion") String modelVersion, 
                                                                  @Param("startDate") LocalDateTime startDate, 
                                                                  @Param("endDate") LocalDateTime endDate);
}
