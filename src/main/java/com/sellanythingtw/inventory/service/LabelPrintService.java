package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.entity.PurchaseLot;
import com.sellanythingtw.inventory.repository.LabelPrintSettingRepository;
import com.sellanythingtw.inventory.repository.PurchaseLotRepository;
import com.sellanythingtw.inventory.utils.BarcodeUtils;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.sellanythingtw.inventory.utils.PdfFontUtils;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LabelPrintService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PurchaseLotRepository lotRepository;
    private final LabelPrintSettingRepository settingRepository;
    private final AppProperties appProperties;

    public LabelPrintService(PurchaseLotRepository lotRepository,
                             LabelPrintSettingRepository settingRepository,
                             AppProperties appProperties) {
        this.lotRepository = lotRepository;
        this.settingRepository = settingRepository;
        this.appProperties = appProperties;
    }

    public LabelPrintSetting getSetting() {
        return getDefaultTemplate();
    }

    public LabelPrintSetting getDefaultTemplate() {
        return settingRepository.findFirstByDefaultTemplateTrueOrderBySettingIdAsc()
                .orElseGet(() -> settingRepository.findAllByOrderByDefaultTemplateDescSettingIdAsc().stream().findFirst()
                        .map(existing -> {
                            existing.setDefaultTemplate(true);
                            return settingRepository.save(existing);
                        })
                        .orElseGet(() -> {
                            LabelPrintSetting setting = new LabelPrintSetting();
                            setting.setTemplateName("預設進貨貼紙");
                            setting.setDefaultTemplate(true);
                            return settingRepository.save(setting);
                        }));
    }

    public List<LabelPrintSetting> listTemplates() {
        return settingRepository.findAllByOrderByDefaultTemplateDescSettingIdAsc();
    }

    public LabelPrintSetting getTemplate(Long settingId) {
        if (settingId == null) return getDefaultTemplate();
        return settingRepository.findById(settingId).orElseGet(this::getDefaultTemplate);
    }

    @Transactional
    public LabelPrintSetting createTemplate() {
        LabelPrintSetting setting = new LabelPrintSetting();
        setting.setTemplateName("進貨貼紙範本 " + (settingRepository.count() + 1));
        setting.setDefaultTemplate(settingRepository.count() == 0);
        return settingRepository.save(setting);
    }

    @Transactional
    public LabelPrintSetting updateSetting(LabelPrintSetting form) {
        boolean creating = form.getSettingId() == null;
        LabelPrintSetting setting = creating ? new LabelPrintSetting() : getTemplate(form.getSettingId());
        setting.setTemplateName(blankToDefault(form.getTemplateName(), "未命名範本"));
        setting.setLabelWidthMm(form.getLabelWidthMm());
        setting.setLabelHeightMm(form.getLabelHeightMm());
        setting.setMarginTopMm(form.getMarginTopMm());
        setting.setMarginLeftMm(form.getMarginLeftMm());
        setting.setFontSizeLarge(form.getFontSizeLarge());
        setting.setFontSizeNormal(form.getFontSizeNormal());
        setting.setBarcodeWidthMm(form.getBarcodeWidthMm());
        setting.setBarcodeHeightMm(form.getBarcodeHeightMm());
        setting.setShowBorder(Boolean.TRUE.equals(form.getShowBorder()));
        if (Boolean.TRUE.equals(form.getDefaultTemplate()) || settingRepository.count() == 0) {
            for (LabelPrintSetting existing : settingRepository.findAll()) {
                existing.setDefaultTemplate(false);
                settingRepository.save(existing);
            }
            setting.setDefaultTemplate(true);
        } else if (setting.getDefaultTemplate() == null || creating) {
            setting.setDefaultTemplate(false);
        }
        LabelPrintSetting saved = settingRepository.save(setting);
        if (settingRepository.findFirstByDefaultTemplateTrueOrderBySettingIdAsc().isEmpty()) {
            saved.setDefaultTemplate(true);
            saved = settingRepository.save(saved);
        }
        return saved;
    }

    @Transactional
    public void deleteTemplate(Long settingId) {
        LabelPrintSetting setting = getTemplate(settingId);
        if (Boolean.TRUE.equals(setting.getDefaultTemplate())) {
            throw new IllegalArgumentException("預設範本不可刪除，請先指定其他範本為預設");
        }
        settingRepository.delete(setting);
    }

    @Transactional
    public String createLotLabelPdf(Long lotId) {
        PurchaseLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨批次"));
        Path path = Path.of(appProperties.getLabelDir(), "purchase", lot.getBarcodeValue() + "_" + LocalDateTime.now().format(TS) + ".pdf");
        FilesUtils.createFolder(path.getParent().toString());
        writeLabels(path, List.of(lot), 1);
        lot.setLabelPrintedCount(safe(lot.getLabelPrintedCount()) + 1);
        lotRepository.save(lot);
        return path.toString();
    }

    @Transactional
    public String createPurchaseLabelsPdf(Long purchaseId, Integer copies) {
        int printCopies = copies == null || copies <= 0 ? 1 : copies;
        List<PurchaseLot> lots = lotRepository.findByPurchaseId(purchaseId);
        if (lots.isEmpty()) throw new IllegalArgumentException("此進貨單尚未建立批次，無法列印貼紙");
        Path path = Path.of(appProperties.getLabelDir(), "purchase", "purchase_" + purchaseId + "_labels.pdf");
        FilesUtils.createFolder(path.getParent().toString());
        writeLabels(path, lots, printCopies);
        return path.toString();
    }

    private void writeLabels(Path path, List<PurchaseLot> lots, int copies) {
        try {
            LabelPrintSetting firstSetting = lots.isEmpty() ? getDefaultTemplate() : getTemplate(lots.get(0).getLabelSettingId());
            Document doc = new Document(labelRectangle(firstSetting), 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();

            BaseFont font = createBaseFont();
            boolean firstPage = true;
            for (PurchaseLot lot : lots) {
                LabelPrintSetting setting = getTemplate(lot.getLabelSettingId());
                for (int i = 0; i < copies; i++) {
                    doc.setPageSize(labelRectangle(setting));
                    if (!firstPage) doc.newPage();
                    firstPage = false;
                    drawLabel(writer, lot, setting, font);
                }
            }
            doc.close();
        } catch (Exception ex) {
            throw new RuntimeException("產生標籤 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    private Rectangle labelRectangle(LabelPrintSetting setting) {
        return new Rectangle(mm(setting.getLabelWidthMm()), mm(setting.getLabelHeightMm()));
    }

    private void drawLabel(PdfWriter writer, PurchaseLot lot, LabelPrintSetting setting, BaseFont font) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        float width = mm(setting.getLabelWidthMm());
        float height = mm(setting.getLabelHeightMm());
        float left = Math.max(mm(2.5), mm(setting.getMarginLeftMm()));
        float right = width - Math.max(mm(2.5), mm(setting.getMarginLeftMm()));
        float top = height - Math.max(mm(7.0), mm(setting.getMarginTopMm()) + mm(4.0));
        float large = Math.min(safeFont(setting.getFontSizeLarge(), 11), 11);
        float normal = Math.min(safeFont(setting.getFontSizeNormal(), 8), 8);
        float small = Math.max(5.5f, normal - 1.2f);

        if (Boolean.TRUE.equals(setting.getShowBorder())) {
            cb.saveState();
            cb.setLineWidth(0.4f);
            cb.rectangle(mm(1.0), mm(1.0), width - mm(2.0), height - mm(2.0));
            cb.stroke();
            cb.restoreState();
        }

        drawText(cb, font, String.valueOf(nvl(lot.getWholesalePrice())), left, top, large, PdfContentByte.ALIGN_LEFT, 8);
        drawText(cb, font, nvl(lot.getTrayQuantityCode()), width * 0.58f, top, normal + 1, PdfContentByte.ALIGN_LEFT, 8);
        drawText(cb, font, nvl(lot.getSizeCode()).toUpperCase(), right, top, normal + 1, PdfContentByte.ALIGN_RIGHT, 8);
        drawText(cb, font, nvl(lot.getPurchaseDateCode()), left, top - mm(5.0), normal, PdfContentByte.ALIGN_LEFT, 8);
        drawText(cb, font, nvl(lot.getProductName()), left, top - mm(9.2), normal + 0.8f, PdfContentByte.ALIGN_LEFT, 18);
        drawText(cb, font, "新品" + nvl(lot.getSalePrice()) + "元 " + nvl(lot.getProductAlias()), left, top - mm(13.0), normal, PdfContentByte.ALIGN_LEFT, 18);
        drawText(cb, font, nvl(lot.getSupplierCode()), left, top - mm(16.8), normal, PdfContentByte.ALIGN_LEFT, 12);

        float barcodeW = Math.min(mm(setting.getBarcodeWidthMm()), width - left - mm(3.0));
        float barcodeH = Math.min(mm(setting.getBarcodeHeightMm()), Math.max(mm(5.5), height * 0.21f));
        float barcodeX = left;
        float barcodeY = mm(5.4);
        byte[] barcodePng = BarcodeUtils.createCode128Png(lot.getBarcodeValue(), 520, 90);
        Image image = Image.getInstance(barcodePng);
        image.scaleToFit(barcodeW, barcodeH);
        image.setAbsolutePosition(barcodeX, barcodeY);
        cb.addImage(image);
        drawText(cb, font, nvl(lot.getBarcodeValue()), barcodeX, mm(2.2), small, PdfContentByte.ALIGN_LEFT, 18);
    }

    private void drawText(PdfContentByte cb, BaseFont font, String text, float x, float y, float size, int align, int max) {
        cb.beginText();
        cb.setFontAndSize(font, size);
        cb.showTextAligned(align, fitText(text, max), x, y, 0);
        cb.endText();
    }

    private BaseFont createBaseFont() {
        return PdfFontUtils.baseFont();
    }

    private float mm(Double value) { return (float) ((value == null ? 0.0 : value) * 72.0 / 25.4); }
    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
    private int safe(Integer value) { return value == null ? 0 : value; }
    private float safeFont(Integer value, int fallback) { return value == null || value <= 0 ? fallback : value; }
    private String fitText(String value, int max) {
        String text = value == null ? "" : value;
        return text.length() <= max ? text : text.substring(0, max);
    }
    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
