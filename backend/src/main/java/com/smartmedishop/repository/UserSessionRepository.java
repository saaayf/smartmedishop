package com.smartmedishop.repository;

import com.smartmedishop.entity.User;
import com.smartmedishop.entity.UserSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    // Find session by token
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    // Find sessions by user
    List<UserSession> findByUser(User user);
    
    // Find sessions by user ID
    List<UserSession> findByUserId(Long userId);
    
    // Find active sessions
    List<UserSession> findByIsActiveTrue();
    
    // Find sessions by user and active status
    List<UserSession> findByUserAndIsActive(User user, Boolean isActive);
    
    // Find sessions by IP address
    List<UserSession> findByIpAddress(String ipAddress);
    
    // Find sessions by user agent
    List<UserSession> findByUserAgent(String userAgent);
    
    // Find sessions by date range
    List<UserSession> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find sessions by expiration date
    List<UserSession> findByExpiresAtBefore(LocalDateTime date);
    
    // Find sessions by last activity
    List<UserSession> findByLastActivityAfter(LocalDateTime date);
    
    // Find expired sessions
    @Query("SELECT us FROM UserSession us WHERE us.expiresAt < :currentTime")
    List<UserSession> findExpiredSessions(@Param("currentTime") LocalDateTime currentTime);
    
    // Find active sessions by user
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true")
    List<UserSession> findActiveSessionsByUser(@Param("userId") Long userId);
    
    // Find sessions by user and IP
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.ipAddress = :ipAddress")
    List<UserSession> findSessionsByUserAndIp(@Param("userId") Long userId, @Param("ipAddress") String ipAddress);
    
    // Count active sessions by user
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true")
    Long countActiveSessionsByUser(@Param("userId") Long userId);
    
    // Find sessions by user and date range
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.createdAt BETWEEN :startDate AND :endDate")
    List<UserSession> findSessionsByUserAndDateRange(@Param("userId") Long userId, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    // Find sessions by IP and date range
    @Query("SELECT us FROM UserSession us WHERE us.ipAddress = :ipAddress AND us.createdAt BETWEEN :startDate AND :endDate")
    List<UserSession> findSessionsByIpAndDateRange(@Param("ipAddress") String ipAddress, 
                                                    @Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);
    
    // Find sessions by user agent and date range
    @Query("SELECT us FROM UserSession us WHERE us.userAgent = :userAgent AND us.createdAt BETWEEN :startDate AND :endDate")
    List<UserSession> findSessionsByUserAgentAndDateRange(@Param("userAgent") String userAgent, 
                                                           @Param("startDate") LocalDateTime startDate, 
                                                           @Param("endDate") LocalDateTime endDate);
    
    // Get session statistics
    @Query("SELECT COUNT(us), AVG(TIMESTAMPDIFF(HOUR, us.createdAt, us.expiresAt)) FROM UserSession us WHERE us.isActive = true")
    Object[] getSessionStatistics();
    
    // Get session statistics by user
    @Query("SELECT COUNT(us), AVG(TIMESTAMPDIFF(HOUR, us.createdAt, us.expiresAt)) FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true")
    Object[] getSessionStatisticsByUser(@Param("userId") Long userId);
    
    // Find sessions by user and user agent
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.userAgent = :userAgent")
    List<UserSession> findSessionsByUserAndUserAgent(@Param("userId") Long userId, @Param("userAgent") String userAgent);
    
    // Find sessions by user and last activity
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.lastActivity > :date")
    List<UserSession> findSessionsByUserAndLastActivity(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    // Find sessions by IP and user agent
    @Query("SELECT us FROM UserSession us WHERE us.ipAddress = :ipAddress AND us.userAgent = :userAgent")
    List<UserSession> findSessionsByIpAndUserAgent(@Param("ipAddress") String ipAddress, @Param("userAgent") String userAgent);
    
    // Find sessions by user and IP and user agent
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.ipAddress = :ipAddress AND us.userAgent = :userAgent")
    List<UserSession> findSessionsByUserAndIpAndUserAgent(@Param("userId") Long userId, 
                                                          @Param("ipAddress") String ipAddress, 
                                                          @Param("userAgent") String userAgent);
}
