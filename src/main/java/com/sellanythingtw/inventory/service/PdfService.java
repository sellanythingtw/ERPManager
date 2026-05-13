package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.entity.*;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
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

            Document doc = new Document(PageSize.A4, 36, 36, 34, 34);
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();

            Font title = font(18, Font.BOLD);
            Font header = font(10, Font.BOLD);
            Font normal = font(10, Font.NORMAL);
            Font small = font(9, Font.NORMAL);

            Paragraph p = new Paragraph("進貨單", title);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
            Paragraph no = new Paragraph(order.getPurchaseNo(), normal);
            no.setAlignment(Element.ALIGN_CENTER);
            no.setSpacingAfter(14);
            doc.add(no);

            PdfPTable info = new PdfPTable(new float[]{1.2f, 2.8f, 1.2f, 2.8f});
            info.setWidthPercentage(100);
            addLabelValue(info, "單據日期", format(order.getDocumentDate()), header, normal);
            addLabelValue(info, "進貨日期", format(order.getPurchaseDate()), header, normal);
            addLabelValue(info, "供應商", supplierName, header, normal);
            addLabelValue(info, "供應商代號", supplier == null ? "" : supplier.getSupplierCode(), header, normal);
            addLabelValue(info, "電話", supplier == null ? "" : supplier.getPhone(), header, normal);
            addLabelValue(info, "聯絡人", supplier == null ? "" : supplier.getContactPerson(), header, normal);
            addLabelValue(info, "地址", supplier == null ? "" : supplier.getAddress(), header, normal, 3);
            doc.add(info);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{0.6f, 1.4f, 2.2f, 1.4f, 1.4f, 1.1f, 1.1f, 0.8f, 1.2f});
            table.setWidthPercentage(100);
            addHeader(table, header, "#", "品號", "品名", "小名", "批發價", "銷售價", "數量", "P/尺寸", "小計");
            int idx = 1;
            for (PurchaseOrderItem item : items) {
                addCell(table, String.valueOf(idx++), small);
                addCell(table, nvl(item.getProductCode()), small);
                addCell(table, nvl(item.getProductName()), small);
                addCell(table, nvl(item.getProductAlias()), small);
                addCell(table, money(item.getWholesalePrice()), small, Element.ALIGN_RIGHT);
                addCell(table, money(item.getSalePrice()), small, Element.ALIGN_RIGHT);
                addCell(table, String.valueOf(item.getQuantity()), small, Element.ALIGN_RIGHT);
                addCell(table, nvl(item.getTrayQuantityCode()) + " / " + nvl(item.getSizeCode()), small);
                addCell(table, money(item.getAmount()), small, Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(new float[]{3f, 1.2f});
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addLabelValue(totals, "總數量", String.valueOf(order.getTotalQuantity()), header, normal);
            addLabelValue(totals, "未稅總額", money(order.getSubtotalAmount()), header, normal);
            addLabelValue(totals, "稅額", money(order.getTaxAmount()), header, normal);
            addLabelValue(totals, "總金額", money(order.getTotalAmount()), header, normal);
            doc.add(totals);

            if (order.getNote() != null && !order.getNote().isBlank()) {
                doc.add(new Paragraph("備註：" + order.getNote(), normal));
            }
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

            Document doc = new Document(PageSize.A4, 36, 36, 34, 34);
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();

            Font title = font(18, Font.BOLD);
            Font header = font(10, Font.BOLD);
            Font normal = font(10, Font.NORMAL);
            Font small = font(9, Font.NORMAL);

            Paragraph p = new Paragraph("銷貨單", title);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
            Paragraph no = new Paragraph(order.getSalesNo(), normal);
            no.setAlignment(Element.ALIGN_CENTER);
            no.setSpacingAfter(14);
            doc.add(no);

            PdfPTable info = new PdfPTable(new float[]{1.2f, 2.8f, 1.2f, 2.8f});
            info.setWidthPercentage(100);
            addLabelValue(info, "單據日期", format(order.getDocumentDate()), header, normal);
            addLabelValue(info, "銷貨日期", format(order.getSalesDate()), header, normal);
            addLabelValue(info, "客戶", customerName, header, normal);
            addLabelValue(info, "客戶代號", customer == null ? "" : customer.getCustomerCode(), header, normal);
            addLabelValue(info, "電話", customer == null ? "" : customer.getPhone(), header, normal);
            addLabelValue(info, "聯絡人", customer == null ? "" : customer.getContactPerson(), header, normal);
            addLabelValue(info, "送貨地址", customer == null ? "" : customer.getShippingAddress(), header, normal, 3);
            addLabelValue(info, "付款類型", nvl(order.getPaymentType()), header, normal);
            addLabelValue(info, "付款狀態", nvl(order.getPaymentStatus()), header, normal);
            doc.add(info);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{0.5f, 1.8f, 1.2f, 2f, 1.2f, 1.1f, 0.8f, 1.2f});
            table.setWidthPercentage(100);
            addHeader(table, header, "#", "條碼", "品號", "品名", "小名", "單價", "數量", "小計");
            int idx = 1;
            for (SalesOrderItem item : items) {
                addCell(table, String.valueOf(idx++), small);
                addCell(table, nvl(item.getBarcodeValue()), small);
                addCell(table, nvl(item.getProductCode()), small);
                addCell(table, nvl(item.getProductName()), small);
                addCell(table, nvl(item.getProductAlias()), small);
                addCell(table, money(item.getUnitPrice()), small, Element.ALIGN_RIGHT);
                addCell(table, String.valueOf(item.getQuantity()), small, Element.ALIGN_RIGHT);
                addCell(table, money(item.getAmount()), small, Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(new float[]{3f, 1.2f});
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addLabelValue(totals, "總數量", String.valueOf(order.getTotalQuantity()), header, normal);
            addLabelValue(totals, "未稅總額", money(order.getSubtotalAmount()), header, normal);
            addLabelValue(totals, "稅額", money(order.getTaxAmount()), header, normal);
            addLabelValue(totals, "總金額", money(order.getTotalAmount()), header, normal);
            addLabelValue(totals, "已收", money(order.getPaidAmount()), header, normal);
            addLabelValue(totals, "未收", money(order.getUnpaidAmount()), header, normal);
            doc.add(totals);

            if (order.getNote() != null && !order.getNote().isBlank()) {
                doc.add(new Paragraph("備註：" + order.getNote(), normal));
            }
            doc.close();
            return path.toString();
        } catch (Exception ex) {
            throw new RuntimeException("產生銷貨單 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    private Font font(int size, int style) {
        try {
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception ignored) {
            return new Font(Font.HELVETICA, size, style);
        }
    }

    private void addHeader(PdfPTable table, Font font, String... names) {
        for (String name : names) {
            PdfPCell cell = new PdfPCell(new Phrase(name, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new Color(238, 242, 247));
            cell.setPadding(5f);
            table.addCell(cell);
        }
    }

    private void addLabelValue(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        addLabelValue(table, label, value, labelFont, valueFont, 1);
    }

    private void addLabelValue(PdfPTable table, String label, String value, Font labelFont, Font valueFont, int valueColspan) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setBackgroundColor(new Color(248, 250, 252));
        l.setPadding(5f);
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "" : value, valueFont));
        v.setPadding(5f);
        v.setColspan(valueColspan);
        table.addCell(v);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        addCell(table, text, font, Element.ALIGN_LEFT);
    }

    private void addCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private String format(java.time.LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
