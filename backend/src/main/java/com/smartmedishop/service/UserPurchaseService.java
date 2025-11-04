package com.smartmedishop.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmedishop.dto.UserPurchaseDto;
import com.smartmedishop.entity.Product;
import com.smartmedishop.entity.Transaction;
import com.smartmedishop.entity.User;
import com.smartmedishop.entity.UserPurchase;
import com.smartmedishop.repository.ProductRepository;
import com.smartmedishop.repository.TransactionRepository;
import com.smartmedishop.repository.UserPurchaseRepository;
import com.smartmedishop.repository.UserRepository;

@Service
public class UserPurchaseService {

    @Autowired
    private UserPurchaseRepository userPurchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public UserPurchase createPurchase(Long userId, Long transactionId, Long productId, 
                                      Integer quantity, String location) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Use location as state (state stores the location)
        String state = location != null ? location : transaction.getLocationCountry();

        UserPurchase purchase = new UserPurchase(
            user,
            transaction,
            product.getId(),
            product.getName(),
            product.getMarque(),
            product.getType(),
            state,
            BigDecimal.valueOf(product.getPrice()),
            quantity
        );

        return userPurchaseRepository.save(purchase);
    }

    @Transactional
    public List<UserPurchase> createPurchasesForTransaction(Long userId, Long transactionId, 
                                                           List<PurchaseItem> items, String location) {
        return items.stream()
            .map(item -> createPurchase(userId, transactionId, item.getProductId(), 
                                       item.getQuantity(), location))
            .collect(Collectors.toList());
    }

    public List<UserPurchaseDto> getUserPurchases(Long userId) {
        List<UserPurchase> purchases = userPurchaseRepository.findByUserIdOrderByPurchaseDateDesc(userId);
        return purchases.stream()
            .map(UserPurchaseDto::new)
            .collect(Collectors.toList());
    }

    public List<UserPurchaseDto> getTransactionPurchases(Long transactionId) {
        List<UserPurchase> purchases = userPurchaseRepository.findByTransactionId(transactionId);
        return purchases.stream()
            .map(UserPurchaseDto::new)
            .collect(Collectors.toList());
    }

    // Inner class for purchase items
    public static class PurchaseItem {
        private Long productId;
        private Integer quantity;

        public PurchaseItem() {}

        public PurchaseItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}

