package com.gigastone.inventory.service;

import com.gigastone.inventory.config.AppProperties;
import com.gigastone.inventory.entity.*;
import com.gigastone.inventory.utils.DateUtils;
import com.gigastone.inventory.utils.FilesUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

@Service
public class PdfService {
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
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();
            doc.add(new Paragraph("進貨單 " + order.getPurchaseNo()));
            doc.add(new Paragraph("供應商：" + supplierName));
            doc.add(new Paragraph("進貨日期：" + order.getPurchaseDate()));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(7);
            addHeader(table, "品號", "品名", "小名", "批發價", "銷售價", "數量", "小計");
            for (PurchaseOrderItem item : items) {
                table.addCell(nvl(item.getProductCode()));
                table.addCell(nvl(item.getProductName()));
                table.addCell(nvl(item.getProductAlias()));
                table.addCell(String.valueOf(item.getWholesalePrice()));
                table.addCell(String.valueOf(item.getSalePrice()));
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell(String.valueOf(item.getAmount()));
            }
            doc.add(table);
            doc.add(new Paragraph("未稅總額：" + order.getSubtotalAmount()));
            doc.add(new Paragraph("稅額：" + order.getTaxAmount()));
            doc.add(new Paragraph("總金額：" + order.getTotalAmount()));
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
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
            doc.open();
            doc.add(new Paragraph("銷貨單 " + order.getSalesNo()));
            doc.add(new Paragraph("客戶：" + customerName));
            doc.add(new Paragraph("銷貨日期：" + order.getSalesDate()));
            doc.add(new Paragraph("付款狀態：" + order.getPaymentStatus()));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(7);
            addHeader(table, "條碼", "品號", "品名", "小名", "單價", "數量", "小計");
            for (SalesOrderItem item : items) {
                table.addCell(nvl(item.getBarcodeValue()));
                table.addCell(nvl(item.getProductCode()));
                table.addCell(nvl(item.getProductName()));
                table.addCell(nvl(item.getProductAlias()));
                table.addCell(String.valueOf(item.getUnitPrice()));
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell(String.valueOf(item.getAmount()));
            }
            doc.add(table);
            doc.add(new Paragraph("未稅總額：" + order.getSubtotalAmount()));
            doc.add(new Paragraph("稅額：" + order.getTaxAmount()));
            doc.add(new Paragraph("總金額：" + order.getTotalAmount()));
            doc.close();
            return path.toString();
        } catch (Exception ex) {
            throw new RuntimeException("產生銷貨單 PDF 失敗: " + ex.getMessage(), ex);
        }
    }

    private void addHeader(PdfPTable table, String... names) {
        for (String name : names) table.addCell(name);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
