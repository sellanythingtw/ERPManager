package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_lots", indexes = {@Index(name = "idx_lot_barcode", columnList = "barcodeValue", unique = true)})
public class PurchaseLot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lotId;
    private Long purchaseId;
    private Long purchaseItemId;
    private String lotNo;
    @Column(nullable = false, unique = true)
    private String barcodeValue;
    private Long productId;
    private String productCode;
    private String productName;
    private String productAlias;
    private Long supplierId;
    private String supplierCode;
    private LocalDate purchaseDate;
    private String purchaseDateCode;
    private BigDecimal wholesalePrice = BigDecimal.ZERO;
    private BigDecimal salePrice = BigDecimal.ZERO;
    private String trayQuantityCode;
    private String sizeCode;
    private Integer initialQuantity = 0;
    private Integer remainingQuantity = 0;
    private Integer labelPrintedCount = 0;
    private String status = "ACTIVE";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public Long getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Long purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public String getLotNo() { return lotNo; }
    public void setLotNo(String lotNo) { this.lotNo = lotNo; }
    public String getBarcodeValue() { return barcodeValue; }
    public void setBarcodeValue(String barcodeValue) { this.barcodeValue = barcodeValue; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductAlias() { return productAlias; }
    public void setProductAlias(String productAlias) { this.productAlias = productAlias; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getPurchaseDateCode() { return purchaseDateCode; }
    public void setPurchaseDateCode(String purchaseDateCode) { this.purchaseDateCode = purchaseDateCode; }
    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getTrayQuantityCode() { return trayQuantityCode; }
    public void setTrayQuantityCode(String trayQuantityCode) { this.trayQuantityCode = trayQuantityCode; }
    public String getSizeCode() { return sizeCode; }
    public void setSizeCode(String sizeCode) { this.sizeCode = sizeCode; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public Integer getLabelPrintedCount() { return labelPrintedCount; }
    public void setLabelPrintedCount(Integer labelPrintedCount) { this.labelPrintedCount = labelPrintedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
