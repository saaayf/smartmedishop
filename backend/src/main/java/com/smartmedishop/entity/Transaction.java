package com.smartmedishop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
    
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "fraud_score")
    private Double fraudScore = 0.0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel = RiskLevel.LOW;
    
    @Column(name = "is_fraud")
    private Boolean isFraud = false;
    
    @Column(name = "fraud_reasons", columnDefinition = "TEXT")
    private String fraudReasons;
    
    @Column(name = "ml_explanation", columnDefinition = "TEXT")
    private String mlExplanation;
    
    @Column(name = "rule_explanation", columnDefinition = "TEXT")
    private String ruleExplanation;
    
    @Column(name = "conclusion", columnDefinition = "TEXT")
    private String conclusion;
    
    @Column(name = "device_type")
    private String deviceType;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "location_country")
    private String locationCountry;
    
    @Column(name = "merchant_name")
    private String merchantName;
    
    @Column(name = "transaction_type")
    private String transactionType;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<FraudAlert> fraudAlerts;
    
    // Constructors
    public Transaction() {}
    
    public Transaction(User user, BigDecimal amount, String paymentMethod) {
        this.user = user;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    
    public Double getFraudScore() { return fraudScore; }
    public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public Boolean getIsFraud() { return isFraud; }
    public void setIsFraud(Boolean isFraud) { this.isFraud = isFraud; }
    
    public String getFraudReasons() { return fraudReasons; }
    public void setFraudReasons(String fraudReasons) { this.fraudReasons = fraudReasons; }
    
    public String getMlExplanation() { return mlExplanation; }
    public void setMlExplanation(String mlExplanation) { this.mlExplanation = mlExplanation; }
    
    public String getRuleExplanation() { return ruleExplanation; }
    public void setRuleExplanation(String ruleExplanation) { this.ruleExplanation = ruleExplanation; }
    
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public java.util.List<FraudAlert> getFraudAlerts() { return fraudAlerts; }
    public void setFraudAlerts(java.util.List<FraudAlert> fraudAlerts) { this.fraudAlerts = fraudAlerts; }
    
    // Enums
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
    
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
