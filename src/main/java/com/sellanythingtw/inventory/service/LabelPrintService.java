package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.entity.PurchaseLot;
import com.sellanythingtw.inventory.repository.LabelPrintSettingRepository;
import com.sellanythingtw.inventory.repository.PurchaseLotRepository;
import com.sellanythingtw.inventory.utils.BarcodeUtils;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Path;

@Service
public class LabelPrintService {
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

    @Transactional
    public String createLotLabelPdf(Long lotId) {
        PurchaseLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨批次"));
        LabelPrintSetting setting = settingRepository.findAll().stream().findFirst().orElseGet(() -> settingRepository.save(new LabelPrintSetting()));
        try {
            float width = mm(setting.getLabelWidthMm());
            float height = mm(setting.getLabelHeightMm());
            Path path = Path.of(appProperties.getLabelDir(), "purchase", lot.getBarcodeValue() + ".pdf");
            FilesUtils.createFolder(path.getParent().toString());

            Document doc = new Document(new Rectangle(width, height), mm(1), mm(1), mm(1), mm(1));
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();

            Font large = new Font(Font.HELVETICA, setting.getFontSizeLarge(), Font.BOLD);
            Font normal = new Font(Font.HELVETICA, setting.getFontSizeNormal(), Font.NORMAL);

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
            doc.close();

            lot.setLabelPrintedCount(lot.getLabelPrintedCount() + 1);
            lotRepository.save(lot);
            return path.toString();
        } catch (Exception ex) {
            throw new RuntimeException("產生標籤 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    private float mm(double value) { return (float) (value * 72.0 / 25.4); }
    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
}
