package com.smartmedishop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartmedishop.entity.UserPurchase;

public class UserPurchaseDto {
    public Long id;
    public Long userId;
    public Long transactionId;
    public Long stockId;
    public String name;
    public String marque;
    public String type;
    public String state;
    public BigDecimal price;
    public Integer quantity;
    public LocalDateTime purchaseDate;

    public UserPurchaseDto() {}

    public UserPurchaseDto(UserPurchase purchase) {
        this.id = purchase.getId();
        this.userId = purchase.getUser().getId();
        this.transactionId = purchase.getTransaction().getId();
        this.stockId = purchase.getStockId();
        this.name = purchase.getName();
        this.marque = purchase.getMarque();
        this.type = purchase.getType();
        this.state = purchase.getState();
        this.price = purchase.getPrice();
        this.quantity = purchase.getQuantity();
        this.purchaseDate = purchase.getPurchaseDate();
    }
}

