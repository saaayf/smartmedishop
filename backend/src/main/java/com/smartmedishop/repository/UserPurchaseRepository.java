package com.smartmedishop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartmedishop.entity.UserPurchase;

public interface UserPurchaseRepository extends JpaRepository<UserPurchase, Long> {
    List<UserPurchase> findByUserIdOrderByPurchaseDateDesc(Long userId);
    List<UserPurchase> findByTransactionId(Long transactionId);
}

