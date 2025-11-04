package com.smartmedishop.dto;

import java.time.LocalDate;

import com.smartmedishop.entity.Product;

public class ProductDto {
    public Long id;
    public String sku;
    public String name;
    public String description;
    public Integer quantity;
    public Integer lowStockThreshold;
    public Double price;
    public LocalDate expirationDate;
    public String marque;
    public String type;

    public ProductDto() {}

    public ProductDto(Product p) {
        this.id = p.getId();
        this.sku = p.getSku();
        this.name = p.getName();
        this.description = p.getDescription();
        this.quantity = p.getQuantity();
        this.lowStockThreshold = p.getLowStockThreshold();
        this.price = p.getPrice();
        this.expirationDate = p.getExpirationDate();
        this.marque = p.getMarque();
        this.type = p.getType();
    }
}
