package com.gigastone.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers", indexes = {@Index(name = "idx_supplier_code", columnList = "supplierCode", unique = true)})
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;
    @Column(nullable = false, unique = true)
    private String supplierCode;
    @Column(nullable = false)
    private String supplierName;
    private String phone;
    private String address;
    private String contactPerson;
    private String paymentTerms;
    private String currency = "TWD";
    private Boolean taxIncluded = false;
    @Column(length = 1000)
    private String note;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getTaxIncluded() { return taxIncluded; }
    public void setTaxIncluded(Boolean taxIncluded) { this.taxIncluded = taxIncluded; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
