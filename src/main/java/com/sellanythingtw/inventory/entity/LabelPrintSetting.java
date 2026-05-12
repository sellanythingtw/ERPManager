package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "label_print_settings")
public class LabelPrintSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settingId;
    private Double labelWidthMm = 50.0;
    private Double labelHeightMm = 30.0;
    private Double marginTopMm = 2.0;
    private Double marginLeftMm = 2.0;
    private Integer fontSizeLarge = 12;
    private Integer fontSizeNormal = 8;
    private Double barcodeWidthMm = 42.0;
    private Double barcodeHeightMm = 8.0;
    private Boolean showBorder = true;
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate public void touch() { updatedAt = LocalDateTime.now(); }

    public Long getSettingId() { return settingId; }
    public void setSettingId(Long settingId) { this.settingId = settingId; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
