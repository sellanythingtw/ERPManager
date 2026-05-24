package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_record_items")
public class PaymentRecordItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentItemId;
    private Long paymentId;
    private Long salesId;
    private Long salesItemId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productAlias;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private Integer salesQuantity = 0;
    private Integer receivedQuantity = 0;
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(length = 1000)
    private String note;
    private LocalDateTime createdAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getPaymentItemId() { return paymentItemId; }
    public void setPaymentItemId(Long paymentItemId) { this.paymentItemId = paymentItemId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getSalesId() { return salesId; }
    public void setSalesId(Long salesId) { this.salesId = salesId; }
    public Long getSalesItemId() { return salesItemId; }
    public void setSalesItemId(Long salesItemId) { this.salesItemId = salesItemId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductAlias() { return productAlias; }
    public void setProductAlias(String productAlias) { this.productAlias = productAlias; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getSalesQuantity() { return salesQuantity; }
    public void setSalesQuantity(Integer salesQuantity) { this.salesQuantity = salesQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
