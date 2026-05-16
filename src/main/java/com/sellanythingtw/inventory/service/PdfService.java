package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.entity.*;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.sellanythingtw.inventory.utils.PdfFontUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final AppProperties appProperties;

    public PdfService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createPurchaseOrderPdf(PurchaseOrder order, List<PurchaseOrderItem> items, Supplier supplier) {
        try {
            String supplierName = supplier == null ? "未指定供應商" : supplier.getSupplierName();
            String fileName = order.getPurchaseNo() + "_" + FilesUtils.safeFileName(supplierName) + ".pdf";
            Path path = Path.of(appProperties.getPdfDir(), "purchase", fileName);
            FilesUtils.createFolder(path.getParent().toString());

            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 24, 24);
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();

            Font title = font(22, Font.BOLD);
            Font subTitle = font(11, Font.NORMAL);
            Font header = font(10, Font.BOLD);
            Font normal = font(10, Font.NORMAL);
            Font small = font(9, Font.NORMAL);
            Color blue = new Color(37, 99, 235);
            Color lightBlue = new Color(239, 246, 255);
            Color gray = new Color(243, 244, 246);

            PdfPTable titleTable = new PdfPTable(new float[]{2f, 1f});
            titleTable.setWidthPercentage(100);
            PdfPCell titleCell = cleanCell(new Phrase("進貨單", title));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleTable.addCell(titleCell);
            PdfPCell noCell = cleanCell(new Phrase("單號：" + nvl(order.getPurchaseNo()) + "\n狀態：" + statusText(order.getStatus()), subTitle));
            noCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            noCell.setBorder(Rectangle.NO_BORDER);
            titleTable.addCell(noCell);
            titleTable.setSpacingAfter(10);
            doc.add(titleTable);

            PdfPTable info = new PdfPTable(new float[]{1.1f, 2.2f, 1.1f, 2.2f, 1.1f, 2.2f});
            info.setWidthPercentage(100);
            addInfo(info, "單據日期", format(order.getDocumentDate()), header, normal, gray);
            addInfo(info, "進貨日期", format(order.getPurchaseDate()), header, normal, gray);
            addInfo(info, "是否計稅", Boolean.TRUE.equals(order.getTaxEnabled()) ? "是" : "否", header, normal, gray);
            addInfo(info, "供應商", supplierName, header, normal, gray);
            addInfo(info, "供應商代號", supplier == null ? "" : supplier.getSupplierCode(), header, normal, gray);
            addInfo(info, "電話", supplier == null ? "" : supplier.getPhone(), header, normal, gray);
            addInfo(info, "聯絡人", supplier == null ? "" : supplier.getContactPerson(), header, normal, gray);
            addInfo(info, "地址", supplier == null ? "" : supplier.getAddress(), header, normal, gray, 5);
            info.setSpacingAfter(12);
            doc.add(info);

            PdfPTable table = new PdfPTable(new float[]{0.45f, 1.15f, 2.1f, 1.25f, 1.3f, 0.9f, 0.9f, 0.8f, 0.85f, 0.8f, 1.0f});
            table.setWidthPercentage(100);
            addHeader(table, header, blue, "#", "品號", "品名", "小名", "規格", "批發價", "銷售價", "P", "尺寸", "數量", "小計");
            int idx = 1;
            for (PurchaseOrderItem item : items) {
                addCell(table, String.valueOf(idx++), small, Element.ALIGN_CENTER);
                addCell(table, nvl(item.getProductCode()), small);
                addCell(table, nvl(item.getProductName()), small);
                addCell(table, nvl(item.getProductAlias()), small);
                addCell(table, trimJoin(item.getSpecification(), item.getColor(), item.getUnit()), small);
                addCell(table, money(item.getWholesalePrice()), small, Element.ALIGN_RIGHT);
                addCell(table, money(item.getSalePrice()), small, Element.ALIGN_RIGHT);
                addCell(table, nvl(item.getTrayQuantityCode()), small, Element.ALIGN_CENTER);
                addCell(table, nvl(item.getSizeCode()), small, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(item.getQuantity()), small, Element.ALIGN_RIGHT);
                addCell(table, money(item.getAmount()), small, Element.ALIGN_RIGHT);
            }
            table.setSpacingAfter(12);
            doc.add(table);

            PdfPTable bottom = new PdfPTable(new float[]{2f, 1f});
            bottom.setWidthPercentage(100);
            PdfPCell note = cleanCell(new Phrase("備註：" + nvl(order.getNote()), normal));
            note.setMinimumHeight(60f);
            note.setPadding(8f);
            note.setBorderColor(new Color(229, 231, 235));
            bottom.addCell(note);

            PdfPTable totals = new PdfPTable(new float[]{1.2f, 1f});
            totals.setWidthPercentage(100);
            addTotal(totals, "總數量", String.valueOf(safeInt(order.getTotalQuantity())), header, normal, gray);
            addTotal(totals, "未稅總額", money(order.getSubtotalAmount()), header, normal, gray);
            addTotal(totals, "稅額", money(order.getTaxAmount()), header, normal, gray);
            addTotal(totals, "總金額", money(order.getTotalAmount()), header, font(11, Font.BOLD), lightBlue);
            PdfPCell totalWrap = cleanCell(totals);
            totalWrap.setBorder(Rectangle.NO_BORDER);
            bottom.addCell(totalWrap);
            doc.add(bottom);

            doc.add(footer("本單據由 ERPManager 產生｜列印日期：" + format(LocalDate.now()), small));
            doc.close();
            return path.toString();
        } catch (Exception ex) {
            throw new RuntimeException("產生進貨單 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    public String createSalesOrderPdf(SalesOrder order, List<SalesOrderItem> items, Customer customer) {
        try {
            String customerName = customer == null ? "未指定客戶" : customer.getCustomerName();
            String fileName = order.getSalesNo() + "_" + FilesUtils.safeFileName(customerName) + ".pdf";
            Path path = Path.of(appProperties.getPdfDir(), "sales", fileName);
            FilesUtils.createFolder(path.getParent().toString());

            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 24, 24);
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();

            Font title = font(22, Font.BOLD);
            Font subTitle = font(11, Font.NORMAL);
            Font header = font(10, Font.BOLD);
            Font normal = font(10, Font.NORMAL);
            Font small = font(9, Font.NORMAL);
            Color blue = new Color(37, 99, 235);
            Color lightBlue = new Color(239, 246, 255);
            Color gray = new Color(243, 244, 246);

            PdfPTable titleTable = new PdfPTable(new float[]{2f, 1f});
            titleTable.setWidthPercentage(100);
            PdfPCell titleCell = cleanCell(new Phrase("銷貨單", title));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleTable.addCell(titleCell);
            PdfPCell noCell = cleanCell(new Phrase("單號：" + nvl(order.getSalesNo()) + "\n狀態：" + statusText(order.getStatus()), subTitle));
            noCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            noCell.setBorder(Rectangle.NO_BORDER);
            titleTable.addCell(noCell);
            titleTable.setSpacingAfter(10);
            doc.add(titleTable);

            PdfPTable info = new PdfPTable(new float[]{1.1f, 2.2f, 1.1f, 2.2f, 1.1f, 2.2f});
            info.setWidthPercentage(100);
            addInfo(info, "單據日期", format(order.getDocumentDate()), header, normal, gray);
            addInfo(info, "銷貨日期", format(order.getSalesDate()), header, normal, gray);
            addInfo(info, "付款類型", nvl(order.getPaymentType()), header, normal, gray);
            addInfo(info, "客戶", customerName, header, normal, gray);
            addInfo(info, "客戶代號", customer == null ? "" : customer.getCustomerCode(), header, normal, gray);
            addInfo(info, "電話", customer == null ? "" : customer.getPhone(), header, normal, gray);
            addInfo(info, "聯絡人", customer == null ? "" : customer.getContactPerson(), header, normal, gray);
            addInfo(info, "送貨地址", customer == null ? "" : customer.getShippingAddress(), header, normal, gray, 5);
            info.setSpacingAfter(12);
            doc.add(info);

            PdfPTable table = new PdfPTable(new float[]{0.45f, 1.8f, 1.1f, 1.8f, 1.1f, 1.0f, 0.8f, 1.0f});
            table.setWidthPercentage(100);
            addHeader(table, header, blue, "#", "條碼", "品號", "品名", "小名", "單價", "數量", "小計");
            int idx = 1;
            for (SalesOrderItem item : items) {
                addCell(table, String.valueOf(idx++), small, Element.ALIGN_CENTER);
                addCell(table, nvl(item.getBarcodeValue()), small);
                addCell(table, nvl(item.getProductCode()), small);
                addCell(table, nvl(item.getProductName()), small);
                addCell(table, nvl(item.getProductAlias()), small);
                addCell(table, money(item.getUnitPrice()), small, Element.ALIGN_RIGHT);
                addCell(table, String.valueOf(item.getQuantity()), small, Element.ALIGN_RIGHT);
                addCell(table, money(item.getAmount()), small, Element.ALIGN_RIGHT);
            }
            table.setSpacingAfter(12);
            doc.add(table);

            PdfPTable bottom = new PdfPTable(new float[]{2f, 1f});
            bottom.setWidthPercentage(100);
            PdfPCell note = cleanCell(new Phrase("備註：" + nvl(order.getNote()), normal));
            note.setMinimumHeight(60f);
            note.setPadding(8f);
            note.setBorderColor(new Color(229, 231, 235));
            bottom.addCell(note);

            PdfPTable totals = new PdfPTable(new float[]{1.2f, 1f});
            totals.setWidthPercentage(100);
            addTotal(totals, "總數量", String.valueOf(safeInt(order.getTotalQuantity())), header, normal, gray);
            addTotal(totals, "未稅總額", money(order.getSubtotalAmount()), header, normal, gray);
            addTotal(totals, "稅額", money(order.getTaxAmount()), header, normal, gray);
            addTotal(totals, "總金額", money(order.getTotalAmount()), header, font(11, Font.BOLD), lightBlue);
            addTotal(totals, "已收", money(order.getPaidAmount()), header, normal, gray);
            addTotal(totals, "未收", money(order.getUnpaidAmount()), header, normal, gray);
            PdfPCell totalWrap = cleanCell(totals);
            totalWrap.setBorder(Rectangle.NO_BORDER);
            bottom.addCell(totalWrap);
            doc.add(bottom);

            doc.add(footer("本單據由 ERPManager 產生｜列印日期：" + format(LocalDate.now()), small));
            doc.close();
            return path.toString();
        } catch (Exception ex) {
            throw new RuntimeException("產生銷貨單 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    private Font font(int size, int style) {
        return PdfFontUtils.font(size, style);
    }

    private PdfPCell cleanCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(6f);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell cleanCell(PdfPTable table) {
        PdfPCell cell = new PdfPCell(table);
        cell.setPadding(0f);
        return cell;
    }

    private void addHeader(PdfPTable table, Font font, Color color, String... names) {
        Font whiteFont = new Font(font.getBaseFont(), font.getSize(), font.getStyle(), Color.WHITE);
        for (String name : names) {
            PdfPCell cell = cleanCell(new Phrase(name, whiteFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(color);
            cell.setBorderColor(color);
            table.addCell(cell);
        }
    }

    private void addInfo(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color bg) {
        addInfo(table, label, value, labelFont, valueFont, bg, 1);
    }

    private void addInfo(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color bg, int valueColspan) {
        PdfPCell labelCell = cleanCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(bg);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(labelCell);
        PdfPCell valueCell = cleanCell(new Phrase(nvl(value), valueFont));
        valueCell.setColspan(valueColspan);
        table.addCell(valueCell);
    }

    private void addTotal(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color bg) {
        PdfPCell labelCell = cleanCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(bg);
        table.addCell(labelCell);
        PdfPCell valueCell = cleanCell(new Phrase(nvl(value), valueFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(bg);
        table.addCell(valueCell);
    }

    private void addCell(PdfPTable table, String value, Font font) {
        addCell(table, value, font, Element.ALIGN_LEFT);
    }

    private void addCell(PdfPTable table, String value, Font font, int align) {
        PdfPCell cell = cleanCell(new Phrase(nvl(value), font));
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private Paragraph footer(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingBefore(14f);
        return p;
    }

    private String nvl(String value) { return value == null ? "" : value; }
    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
    private String format(LocalDate value) { return value == null ? "" : value.format(DATE); }
    private String money(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
    private int safeInt(Integer value) { return value == null ? 0 : value; }
    private String statusText(String status) {
        if ("CONFIRMED".equals(status)) return "已確認";
        if ("DRAFT".equals(status)) return "草稿";
        if ("VOID".equals(status)) return "作廢";
        return nvl(status);
    }
    private String trimJoin(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (v == null || v.isBlank()) continue;
            if (sb.length() > 0) sb.append(" / ");
            sb.append(v.trim());
        }
        return sb.toString();
    }
}
