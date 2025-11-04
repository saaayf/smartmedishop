package com.smartmedishop.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmedishop.entity.User;
import com.smartmedishop.entity.UserBehavior;
import com.smartmedishop.repository.UserBehaviorRepository;
import com.smartmedishop.repository.UserRepository;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserBehaviorRepository userBehaviorRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // User CRUD operations
    public User createUser(User user) {
        // Hash password
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setRegistrationDate(LocalDateTime.now());
        user.setIsActive(true);
        user.setIsVerified(false);
        user.setRiskProfile(User.RiskProfile.LOW);
        user.setFraudCount(0);
        user.setTotalTransactions(0);
        user.setAverageAmount(0.0);
        
        User savedUser = userRepository.save(user);
        
        // Initialize user behavior
        initializeUserBehavior(savedUser);
        
        // Note: User risk scores are now calculated dynamically, no need to initialize
        
        return savedUser;
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username, email);
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    public List<User> findUsersByType(User.UserType userType) {
        return userRepository.findByUserType(userType);
    }
    
    public List<User> findUsersByRiskProfile(User.RiskProfile riskProfile) {
        return userRepository.findByRiskProfile(riskProfile);
    }
    
    public List<User> findUsersWithFraudHistory() {
        return userRepository.findUsersWithFraudHistory();
    }
    
    public List<User> findHighRiskUsers() {
        return userRepository.findHighRiskUsers();
    }
    
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // User behavior operations
    private void initializeUserBehavior(User user) {
        UserBehavior userBehavior = new UserBehavior(user);
        userBehavior.setTransactionVelocity(0);
        userBehavior.setAmountVelocity(0.0);
        userBehavior.setAverageTransactionAmount(0.0);
        userBehavior.setMaxTransactionAmount(0.0);
        userBehavior.setUnusualPatternsCount(0);
        userBehavior.setTransactionFrequencyPerDay(0.0);
        userBehavior.setWeekendTransactionRatio(0.0);
        userBehavior.setNightTransactionRatio(0.0);
        
        userBehaviorRepository.save(userBehavior);
    }
    
    public UserBehavior getUserBehavior(Long userId) {
        return userBehaviorRepository.findByUserId(userId).orElse(null);
    }
    
    public UserBehavior updateUserBehavior(UserBehavior userBehavior) {
        return userBehaviorRepository.save(userBehavior);
    }
    
    // Note: User risk score operations removed - table was removed
    // Risk scores are now calculated dynamically from user's riskProfile field
    
    // User statistics
    public Long countUsersByType(User.UserType userType) {
        return userRepository.countByUserType(userType);
    }
    
    public List<User> findUsersWithRecentFraud(LocalDateTime since) {
        return userRepository.findUsersWithRecentFraud(since);
    }
    
    // User authentication
    public User updateLastLogin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }
    
    // User risk management
    public User updateUserRiskProfile(Long userId, User.RiskProfile riskProfile) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRiskProfile(riskProfile);
            return userRepository.save(user);
        }
        return null;
    }
    
    public User incrementFraudCount(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFraudCount(user.getFraudCount() + 1);
            return userRepository.save(user);
        }
        return null;
    }
    
    public User updateTransactionStats(Long userId, Double amount) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTotalTransactions(user.getTotalTransactions() + 1);
            
            // Update average amount
            Double currentAverage = user.getAverageAmount();
            Integer totalTransactions = user.getTotalTransactions();
            Double newAverage = ((currentAverage * (totalTransactions - 1)) + amount) / totalTransactions;
            user.setAverageAmount(newAverage);
            
            return userRepository.save(user);
        }
        return null;
    }
    
    // User management methods for admin interface
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable);
    }
    
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Total users
        statistics.put("totalUsers", userRepository.count());
        
        // Active users
        statistics.put("activeUsers", userRepository.countByIsActiveTrue());
        
        // Inactive users
        statistics.put("inactiveUsers", userRepository.countByIsActiveFalse());
        
        // Users by type
        statistics.put("customers", userRepository.countByUserType(User.UserType.CUSTOMER));
        statistics.put("admins", userRepository.countByUserType(User.UserType.ADMIN));
        statistics.put("fraudAnalysts", userRepository.countByUserType(User.UserType.FRAUD_ANALYST));
        statistics.put("suppliers", userRepository.countByUserType(User.UserType.SUPPLIER));
        statistics.put("nurses", userRepository.countByUserType(User.UserType.NURSE));
        statistics.put("deliveryMen", userRepository.countByUserType(User.UserType.DELIVERY_MAN));
        statistics.put("technicalSupport", userRepository.countByUserType(User.UserType.TECHNICAL_SUPPORT));
        
        // Users by risk profile
        statistics.put("lowRiskUsers", userRepository.countByRiskProfile(User.RiskProfile.LOW));
        statistics.put("mediumRiskUsers", userRepository.countByRiskProfile(User.RiskProfile.MEDIUM));
        // High risk users includes both HIGH and CRITICAL risk profiles
        Long highRiskCount = userRepository.countByRiskProfile(User.RiskProfile.HIGH);
        Long criticalRiskCount = userRepository.countByRiskProfile(User.RiskProfile.CRITICAL);
        statistics.put("highRiskUsers", highRiskCount + criticalRiskCount);
        statistics.put("criticalRiskUsers", criticalRiskCount);
        
        // Users with fraud history
        statistics.put("usersWithFraudHistory", userRepository.countUsersWithFraudHistory());
        
        // Verified users
        statistics.put("verifiedUsers", userRepository.countByIsVerifiedTrue());
        statistics.put("unverifiedUsers", userRepository.countByIsVerifiedFalse());
        
        return statistics;
    }
}
