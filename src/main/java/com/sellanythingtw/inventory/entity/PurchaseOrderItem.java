package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;
    private Long purchaseId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productAlias;
    private String specification;
    private String color;
    private String unit;
    private BigDecimal wholesalePrice = BigDecimal.ZERO;
    private BigDecimal salePrice = BigDecimal.ZERO;
    private String trayQuantityCode;
    private String sizeCode;
    private Integer quantity = 0;
    private BigDecimal amount = BigDecimal.ZERO;
    private Integer sortOrder = 0;
    private Long labelSettingId;
    @Column(length = 1000)
    private String itemNote;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
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
    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getTrayQuantityCode() { return trayQuantityCode; }
    public void setTrayQuantityCode(String trayQuantityCode) { this.trayQuantityCode = trayQuantityCode; }
    public String getSizeCode() { return sizeCode; }
    public void setSizeCode(String sizeCode) { this.sizeCode = sizeCode; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Long getLabelSettingId() { return labelSettingId; }
    public void setLabelSettingId(Long labelSettingId) { this.labelSettingId = labelSettingId; }
    public String getItemNote() { return itemNote; }
    public void setItemNote(String itemNote) { this.itemNote = itemNote; }
}
