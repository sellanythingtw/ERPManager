package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "label_print_settings")
public class LabelPrintSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settingId;
    private String templateName = "預設進貨貼紙";
    private Boolean defaultTemplate = true;
    private Double labelWidthMm = 50.0;
    private Double labelHeightMm = 30.0;
    private Double marginTopMm = 3.0;
    private Double marginLeftMm = 3.0;
    private Integer fontSizeLarge = 11;
    private Integer fontSizeNormal = 8;
    private Double barcodeWidthMm = 38.0;
    private Double barcodeHeightMm = 7.0;
    private Boolean showBorder = true;

    // 精準定位欄位：單位為 mm，座標原點為貼紙左下角。空白則使用系統預設座標。
    private Double priceX;
    private Double priceY;
    private Double trayX;
    private Double trayY;
    private Double sizeX;
    private Double sizeY;
    private Double dateX;
    private Double dateY;
    private Double productNameX;
    private Double productNameY;
    private Double saleAliasX;
    private Double saleAliasY;
    private Double supplierCodeX;
    private Double supplierCodeY;
    private Double barcodeX;
    private Double barcodeY;
    private Double barcodeTextX;
    private Double barcodeTextY;
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate public void touch() { updatedAt = LocalDateTime.now(); }

    public Long getSettingId() { return settingId; }
    public void setSettingId(Long settingId) { this.settingId = settingId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Boolean getDefaultTemplate() { return defaultTemplate; }
    public void setDefaultTemplate(Boolean defaultTemplate) { this.defaultTemplate = defaultTemplate; }
    public Double getLabelWidthMm() { return labelWidthMm; }
    public void setLabelWidthMm(Double labelWidthMm) { this.labelWidthMm = labelWidthMm; }
    public Double getLabelHeightMm() { return labelHeightMm; }
    public void setLabelHeightMm(Double labelHeightMm) { this.labelHeightMm = labelHeightMm; }
    public Double getMarginTopMm() { return marginTopMm; }
    public void setMarginTopMm(Double marginTopMm) { this.marginTopMm = marginTopMm; }
    public Double getMarginLeftMm() { return marginLeftMm; }
    public void setMarginLeftMm(Double marginLeftMm) { this.marginLeftMm = marginLeftMm; }
    public Integer getFontSizeLarge() { return fontSizeLarge; }
    public void setFontSizeLarge(Integer fontSizeLarge) { this.fontSizeLarge = fontSizeLarge; }
    public Integer getFontSizeNormal() { return fontSizeNormal; }
    public void setFontSizeNormal(Integer fontSizeNormal) { this.fontSizeNormal = fontSizeNormal; }
    public Double getBarcodeWidthMm() { return barcodeWidthMm; }
    public void setBarcodeWidthMm(Double barcodeWidthMm) { this.barcodeWidthMm = barcodeWidthMm; }
    public Double getBarcodeHeightMm() { return barcodeHeightMm; }
    public void setBarcodeHeightMm(Double barcodeHeightMm) { this.barcodeHeightMm = barcodeHeightMm; }
    public Boolean getShowBorder() { return showBorder; }
    public void setShowBorder(Boolean showBorder) { this.showBorder = showBorder; }

    public Double getPriceX() { return priceX; }
    public void setPriceX(Double priceX) { this.priceX = priceX; }
    public Double getPriceY() { return priceY; }
    public void setPriceY(Double priceY) { this.priceY = priceY; }
    public Double getTrayX() { return trayX; }
    public void setTrayX(Double trayX) { this.trayX = trayX; }
    public Double getTrayY() { return trayY; }
    public void setTrayY(Double trayY) { this.trayY = trayY; }
    public Double getSizeX() { return sizeX; }
    public void setSizeX(Double sizeX) { this.sizeX = sizeX; }
    public Double getSizeY() { return sizeY; }
    public void setSizeY(Double sizeY) { this.sizeY = sizeY; }
    public Double getDateX() { return dateX; }
    public void setDateX(Double dateX) { this.dateX = dateX; }
    public Double getDateY() { return dateY; }
    public void setDateY(Double dateY) { this.dateY = dateY; }
    public Double getProductNameX() { return productNameX; }
    public void setProductNameX(Double productNameX) { this.productNameX = productNameX; }
    public Double getProductNameY() { return productNameY; }
    public void setProductNameY(Double productNameY) { this.productNameY = productNameY; }
    public Double getSaleAliasX() { return saleAliasX; }
    public void setSaleAliasX(Double saleAliasX) { this.saleAliasX = saleAliasX; }
    public Double getSaleAliasY() { return saleAliasY; }
    public void setSaleAliasY(Double saleAliasY) { this.saleAliasY = saleAliasY; }
    public Double getSupplierCodeX() { return supplierCodeX; }
    public void setSupplierCodeX(Double supplierCodeX) { this.supplierCodeX = supplierCodeX; }
    public Double getSupplierCodeY() { return supplierCodeY; }
    public void setSupplierCodeY(Double supplierCodeY) { this.supplierCodeY = supplierCodeY; }
    public Double getBarcodeX() { return barcodeX; }
    public void setBarcodeX(Double barcodeX) { this.barcodeX = barcodeX; }
    public Double getBarcodeY() { return barcodeY; }
    public void setBarcodeY(Double barcodeY) { this.barcodeY = barcodeY; }
    public Double getBarcodeTextX() { return barcodeTextX; }
    public void setBarcodeTextX(Double barcodeTextX) { this.barcodeTextX = barcodeTextX; }
    public Double getBarcodeTextY() { return barcodeTextY; }
    public void setBarcodeTextY(Double barcodeTextY) { this.barcodeTextY = barcodeTextY; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
