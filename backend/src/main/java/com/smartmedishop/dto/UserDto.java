package com.smartmedishop.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smartmedishop.entity.User;

public class UserDto {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLogin;
    private User.UserType userType;
    private Boolean isActive;
    private Boolean isVerified;
    private User.RiskProfile riskProfile;
    private Integer fraudCount;
    private Integer totalTransactions;
    private Double averageAmount;
    
    // Constructors
    public UserDto() {}
    
    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
        this.birthDate = user.getBirthDate();
        this.registrationDate = user.getRegistrationDate();
        this.lastLogin = user.getLastLogin();
        this.userType = user.getUserType();
        this.isActive = user.getIsActive();
        this.isVerified = user.getIsVerified();
        this.riskProfile = user.getRiskProfile();
        this.fraudCount = user.getFraudCount();
        this.totalTransactions = user.getTotalTransactions();
        this.averageAmount = user.getAverageAmount();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public User.UserType getUserType() { return userType; }
    public void setUserType(User.UserType userType) { this.userType = userType; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public User.RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(User.RiskProfile riskProfile) { this.riskProfile = riskProfile; }
    
    public Integer getFraudCount() { return fraudCount; }
    public void setFraudCount(Integer fraudCount) { this.fraudCount = fraudCount; }
    
    public Integer getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; }
    
    public Double getAverageAmount() { return averageAmount; }
    public void setAverageAmount(Double averageAmount) { this.averageAmount = averageAmount; }
}
