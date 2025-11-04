package com.smartmedishop.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_behavior")
public class UserBehavior {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "preferred_payment_method")
    private String preferredPaymentMethod;
    
    @Column(name = "_device_type")
    private String preferredDeviceType;
    
    @Column(name = "location_country")
    private String locationCountry;
    
    @Column(name = "transaction_velocity")
    private Integer transactionVelocity = 0;
    
    @Column(name = "amount_velocity")
    private Double amountVelocity = 0.0;
    
    @Column(name = "average_transaction_amount")
    private Double averageTransactionAmount = 0.0;
    
    @Column(name = "max_transaction_amount")
    private Double maxTransactionAmount = 0.0;
    
    @Column(name = "min_transaction_amount")
    private Double minTransactionAmount = 0.0;
    
    @Column(name = "unusual_patterns_count")
    private Integer unusualPatternsCount = 0;
    
    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;
    
    @Column(name = "transaction_frequency_per_day")
    private Double transactionFrequencyPerDay = 0.0;
    
    @Column(name = "weekend_transaction_ratio")
    private Double weekendTransactionRatio = 0.0;
    
    @Column(name = "night_transaction_ratio")
    private Double nightTransactionRatio = 0.0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // Constructors
    public UserBehavior() {}
    
    public UserBehavior(User user) {
        this.userId = user.getId();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getPreferredPaymentMethod() { return preferredPaymentMethod; }
    public void setPreferredPaymentMethod(String preferredPaymentMethod) { this.preferredPaymentMethod = preferredPaymentMethod; }
    
    public String getPreferredDeviceType() { return preferredDeviceType; }
    public void setPreferredDeviceType(String preferredDeviceType) { this.preferredDeviceType = preferredDeviceType; }
    
    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }
    
    public Integer getTransactionVelocity() { return transactionVelocity; }
    public void setTransactionVelocity(Integer transactionVelocity) { this.transactionVelocity = transactionVelocity; }
    
    public Double getAmountVelocity() { return amountVelocity; }
    public void setAmountVelocity(Double amountVelocity) { this.amountVelocity = amountVelocity; }
    
    public Double getAverageTransactionAmount() { return averageTransactionAmount; }
    public void setAverageTransactionAmount(Double averageTransactionAmount) { this.averageTransactionAmount = averageTransactionAmount; }
    
    public Double getMaxTransactionAmount() { return maxTransactionAmount; }
    public void setMaxTransactionAmount(Double maxTransactionAmount) { this.maxTransactionAmount = maxTransactionAmount; }
    
    public Double getMinTransactionAmount() { return minTransactionAmount; }
    public void setMinTransactionAmount(Double minTransactionAmount) { this.minTransactionAmount = minTransactionAmount; }
    
    public Integer getUnusualPatternsCount() { return unusualPatternsCount; }
    public void setUnusualPatternsCount(Integer unusualPatternsCount) { this.unusualPatternsCount = unusualPatternsCount; }
    
    public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }
    
    public Double getTransactionFrequencyPerDay() { return transactionFrequencyPerDay; }
    public void setTransactionFrequencyPerDay(Double transactionFrequencyPerDay) { this.transactionFrequencyPerDay = transactionFrequencyPerDay; }
    
    public Double getWeekendTransactionRatio() { return weekendTransactionRatio; }
    public void setWeekendTransactionRatio(Double weekendTransactionRatio) { this.weekendTransactionRatio = weekendTransactionRatio; }
    
    public Double getNightTransactionRatio() { return nightTransactionRatio; }
    public void setNightTransactionRatio(Double nightTransactionRatio) { this.nightTransactionRatio = nightTransactionRatio; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
