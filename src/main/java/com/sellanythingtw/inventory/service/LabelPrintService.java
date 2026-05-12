package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.entity.PurchaseLot;
import com.sellanythingtw.inventory.repository.LabelPrintSettingRepository;
import com.sellanythingtw.inventory.repository.PurchaseLotRepository;
import com.sellanythingtw.inventory.utils.BarcodeUtils;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
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
        return settingRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingRepository.save(new LabelPrintSetting()));
    }

    @Transactional
    public LabelPrintSetting updateSetting(LabelPrintSetting form) {
        LabelPrintSetting setting = getSetting();
        setting.setLabelWidthMm(form.getLabelWidthMm());
        setting.setLabelHeightMm(form.getLabelHeightMm());
        setting.setMarginTopMm(form.getMarginTopMm());
        setting.setMarginLeftMm(form.getMarginLeftMm());
        setting.setFontSizeLarge(form.getFontSizeLarge());
        setting.setFontSizeNormal(form.getFontSizeNormal());
        setting.setBarcodeWidthMm(form.getBarcodeWidthMm());
        setting.setBarcodeHeightMm(form.getBarcodeHeightMm());
        setting.setShowBorder(Boolean.TRUE.equals(form.getShowBorder()));
        return settingRepository.save(setting);
    }

    @Transactional
    public String createLotLabelPdf(Long lotId) {
        PurchaseLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨批次"));
        LabelPrintSetting setting = getSetting();
        Path path = Path.of(appProperties.getLabelDir(), "purchase", lot.getBarcodeValue() + ".pdf");
        FilesUtils.createFolder(path.getParent().toString());
        writeLabels(path, List.of(lot), setting, 1);
        lot.setLabelPrintedCount(safe(lot.getLabelPrintedCount()) + 1);
        lotRepository.save(lot);
        return path.toString();
    }

    @Transactional
    public String createPurchaseLabelsPdf(Long purchaseId, Integer copies) {
        int printCopies = copies == null || copies <= 0 ? 1 : copies;
        List<PurchaseLot> lots = lotRepository.findByPurchaseId(purchaseId);
        if (lots.isEmpty()) throw new IllegalArgumentException("此進貨單尚未建立批次，無法列印貼紙");
        LabelPrintSetting setting = getSetting();
        Path path = Path.of(appProperties.getLabelDir(), "purchase", "purchase_" + purchaseId + "_labels_" + LocalDateTime.now().format(TS) + ".pdf");
        FilesUtils.createFolder(path.getParent().toString());
        writeLabels(path, lots, setting, printCopies);
        for (PurchaseLot lot : lots) {
            lot.setLabelPrintedCount(safe(lot.getLabelPrintedCount()) + printCopies);
        }
        lotRepository.saveAll(lots);
        return path.toString();
    }

    private void writeLabels(Path path, List<PurchaseLot> lots, LabelPrintSetting setting, int copies) {
        try {
            float width = mm(setting.getLabelWidthMm());
            float height = mm(setting.getLabelHeightMm());
            Document doc = new Document(new Rectangle(width, height), mm(1), mm(1), mm(1), mm(1));
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();
            Font large = createFont(setting.getFontSizeLarge(), Font.BOLD);
            Font normal = createFont(setting.getFontSizeNormal(), Font.NORMAL);

            boolean firstPage = true;
            for (PurchaseLot lot : lots) {
                for (int i = 0; i < copies; i++) {
                    if (!firstPage) doc.newPage();
                    firstPage = false;
                    addLabelPage(doc, lot, setting, large, normal);
                }
            }
            doc.close();
        } catch (Exception ex) {
            throw new RuntimeException("產生標籤 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    private void addLabelPage(Document doc, PurchaseLot lot, LabelPrintSetting setting, Font large, Font normal) throws Exception {
        doc.add(new Paragraph(nvl(lot.getWholesalePrice()) + "        " + nvl(lot.getTrayQuantityCode()) + "   " + nvl(lot.getSizeCode()), large));
        doc.add(new Paragraph(nvl(lot.getPurchaseDateCode()), normal));
        doc.add(new Paragraph(nvl(lot.getProductName()), large));
        doc.add(new Paragraph("新品" + nvl(lot.getSalePrice()) + "元 " + nvl(lot.getProductAlias()), normal));
        doc.add(new Paragraph(nvl(lot.getSupplierCode()), normal));

        byte[] barcodePng = BarcodeUtils.createCode128Png(lot.getBarcodeValue(), 360, 70);
        Image image = Image.getInstance(barcodePng);
        image.scaleToFit(mm(setting.getBarcodeWidthMm()), mm(setting.getBarcodeHeightMm()));
        doc.add(image);
        doc.add(new Paragraph(lot.getBarcodeValue(), normal));
    }

    private Font createFont(Integer size, int style) {
        int fontSize = size == null ? 9 : size;
        try {
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(bf, fontSize, style);
        } catch (Exception ignored) {
            return new Font(Font.HELVETICA, fontSize, style);
        }
    }

    private float mm(double value) { return (float) (value * 72.0 / 25.4); }
    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
    private int safe(Integer value) { return value == null ? 0 : value; }
}
