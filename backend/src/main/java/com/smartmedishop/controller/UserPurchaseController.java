package com.smartmedishop.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.dto.UserPurchaseDto;
import com.smartmedishop.security.CustomUserDetailsService;
import com.smartmedishop.service.UserPurchaseService;
import com.smartmedishop.service.UserPurchaseService.PurchaseItem;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*")
public class UserPurchaseController {

    @Autowired
    private UserPurchaseService userPurchaseService;

    @PostMapping("/record")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> recordPurchases(@RequestBody Map<String, Object> request,
                                            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            Long transactionId = Long.parseLong(request.get("transactionId").toString());
            String location = request.get("location") != null ? request.get("location").toString() : null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) request.get("items");
            
            List<PurchaseItem> items = itemsData.stream()
                .map(item -> {
                    Long productId = Long.parseLong(item.get("productId").toString());
                    Integer quantity = Integer.parseInt(item.get("quantity").toString());
                    return new PurchaseItem(productId, quantity);
                })
                .collect(Collectors.toList());

            List<com.smartmedishop.entity.UserPurchase> purchases = 
                userPurchaseService.createPurchasesForTransaction(userId, transactionId, items, location);

            List<UserPurchaseDto> dtos = purchases.stream()
                .map(p -> new UserPurchaseDto(p))
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-purchases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyPurchases(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();

            List<UserPurchaseDto> purchases = userPurchaseService.getUserPurchases(userId);
            return ResponseEntity.ok(purchases);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getUserPurchases(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        try {
            List<UserPurchaseDto> purchases = userPurchaseService.getUserPurchases(userId);
            return ResponseEntity.ok(purchases);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTransactionPurchases(@org.springframework.web.bind.annotation.PathVariable Long transactionId) {
        try {
            List<UserPurchaseDto> purchases = userPurchaseService.getTransactionPurchases(transactionId);
            return ResponseEntity.ok(purchases);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

