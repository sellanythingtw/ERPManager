package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {@Index(name = "idx_product_code", columnList = "productCode", unique = true)})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    @Column(nullable = false, unique = true)
    private String productCode;
    private String category;
    @Column(nullable = false)
    private String productName;
    private String productAlias;
    private String specification;
    private String color;
    private String unit;
    private Integer safetyStock = 0;
    private BigDecimal defaultPurchasePrice = BigDecimal.ZERO;
    private BigDecimal defaultSalePrice = BigDecimal.ZERO;
    private Long supplierId;
    private String supplierProductCode;
    private String barcode;
    @Column(length = 1000)
    private String note;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductAlias() { return productAlias; }
    public void setProductAlias(String productAlias) { this.productAlias = productAlias; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }
    public BigDecimal getDefaultPurchasePrice() { return defaultPurchasePrice; }
    public void setDefaultPurchasePrice(BigDecimal defaultPurchasePrice) { this.defaultPurchasePrice = defaultPurchasePrice; }
    public BigDecimal getDefaultSalePrice() { return defaultSalePrice; }
    public void setDefaultSalePrice(BigDecimal defaultSalePrice) { this.defaultSalePrice = defaultSalePrice; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierProductCode() { return supplierProductCode; }
    public void setSupplierProductCode(String supplierProductCode) { this.supplierProductCode = supplierProductCode; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
