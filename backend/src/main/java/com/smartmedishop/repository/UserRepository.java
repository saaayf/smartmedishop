package com.smartmedishop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmedishop.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Find by email
    Optional<User> findByEmail(String email);
    
    // Find by username or email
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    // Find active users
    List<User> findByIsActiveTrue();
    
    // Find users by type
    List<User> findByUserType(User.UserType userType);
    
    // Find users by risk profile
    List<User> findByRiskProfile(User.RiskProfile riskProfile);
    
    // Find users with fraud history
    @Query("SELECT u FROM User u WHERE u.fraudCount > 0")
    List<User> findUsersWithFraudHistory();
    
    // Find users registered after date
    List<User> findByRegistrationDateAfter(LocalDateTime date);
    
    // Find users by last login
    List<User> findByLastLoginAfter(LocalDateTime date);
    
    // Count users by type
    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :userType")
    Long countByUserType(@Param("userType") User.UserType userType);
    
    // Find users with high risk
    @Query("SELECT u FROM User u WHERE u.riskProfile IN ('HIGH', 'CRITICAL')")
    List<User> findHighRiskUsers();
    
    // Find users with recent fraud
    @Query("SELECT u FROM User u WHERE u.fraudCount > 0 AND u.lastLogin > :since")
    List<User> findUsersWithRecentFraud(@Param("since") LocalDateTime since);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Count methods for statistics
    Long countByIsActiveTrue();
    Long countByIsActiveFalse();
    Long countByRiskProfile(User.RiskProfile riskProfile);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.fraudCount > 0")
    Long countUsersWithFraudHistory();
    
    Long countByIsVerifiedTrue();
    Long countByIsVerifiedFalse();
    
    // Search users with pagination
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
