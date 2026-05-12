package com.gigastone.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long purchaseId;
    @Column(nullable = false, unique = true)
    private String purchaseNo;
    private LocalDate documentDate;
    private LocalDate purchaseDate;
    private Long supplierId;
    private Boolean taxEnabled = false;
    private BigDecimal taxRate = new BigDecimal("0.05");
    private BigDecimal subtotalAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private Integer totalQuantity = 0;
    private String status = "DRAFT";
    private String pdfPath;
    private String cloudPdfUrl;
    private String cloudUploadStatus = "NOT_UPLOADED";
    @Column(length = 1000)
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Boolean getTaxEnabled() { return taxEnabled; }
    public void setTaxEnabled(Boolean taxEnabled) { this.taxEnabled = taxEnabled; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public String getCloudPdfUrl() { return cloudPdfUrl; }
    public void setCloudPdfUrl(String cloudPdfUrl) { this.cloudPdfUrl = cloudPdfUrl; }
    public String getCloudUploadStatus() { return cloudUploadStatus; }
    public void setCloudUploadStatus(String cloudUploadStatus) { this.cloudUploadStatus = cloudUploadStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
