package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.PurchaseOrder;
import com.sellanythingtw.inventory.entity.SalesOrder;
import com.sellanythingtw.inventory.repository.PurchaseOrderRepository;
import com.sellanythingtw.inventory.repository.SalesOrderRepository;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.sellanythingtw.inventory.utils.MsgUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class CloudSyncService {
    private final AppSettingService settings;
    private final GoogleDriveService googleDriveService;
    private final InventoryService inventoryService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;

    public CloudSyncService(AppSettingService settings,
                            GoogleDriveService googleDriveService,
                            InventoryService inventoryService,
                            PurchaseOrderRepository purchaseOrderRepository,
                            SalesOrderRepository salesOrderRepository) {
        this.settings = settings;
        this.googleDriveService = googleDriveService;
        this.inventoryService = inventoryService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.salesOrderRepository = salesOrderRepository;
    }

    public void uploadPdfIfEnabled(String sourceType, Long sourceId, String localFilePath, String ignoredFolderName) {
        try {
            if (!settings.getBoolean("drive.enabled") || !googleDriveService.isReady()) {
                MsgUtils.printMsg("[雲端同步] 未啟用或未連接 Google Drive，略過 PDF 上傳: " + sourceType + " / " + sourceId);
                return;
            }
            boolean pdfUploaded = false;
            if ("PURCHASE".equals(sourceType) && settings.getBoolean("drive.upload.purchasePdf")) {
                PurchaseOrder order = purchaseOrderRepository.findById(sourceId).orElse(null);
                if (order == null) return;
                String fileName = safeFileName(order.getPurchaseNo()) + ".pdf";
                var result = googleDriveService.uploadOrUpdate("進貨單", localFilePath, order.getDriveFileId(), fileName);
                order.setDriveFileId(result.fileId());
                order.setDriveFileName(result.fileName());
                order.setDriveFolderId(result.folderId());
                order.setDriveUploadedAt(result.uploadedAt());
                purchaseOrderRepository.save(order);
                MsgUtils.printMsg("[雲端同步] 進貨單 PDF 已更新: " + fileName);
                pdfUploaded = true;
            }
            if ("SALES".equals(sourceType) && settings.getBoolean("drive.upload.salesPdf")) {
                SalesOrder order = salesOrderRepository.findById(sourceId).orElse(null);
                if (order == null) return;
                String fileName = safeFileName(order.getSalesNo()) + ".pdf";
                var result = googleDriveService.uploadOrUpdate("銷貨單", localFilePath, order.getDriveFileId(), fileName);
                order.setDriveFileId(result.fileId());
                order.setDriveFileName(result.fileName());
                order.setDriveFolderId(result.folderId());
                order.setDriveUploadedAt(result.uploadedAt());
                salesOrderRepository.save(order);
                MsgUtils.printMsg("[雲端同步] 銷貨單 PDF 已更新: " + fileName);
                pdfUploaded = true;
            }
            if ("PURCHASE".equals(sourceType) || "SALES".equals(sourceType)) {
                uploadRealtimeInventoryIfEnabled();
            }
            if (!pdfUploaded) MsgUtils.printMsg("[雲端同步] PDF 自動上傳未啟用，僅檢查庫存同步: " + sourceType + " / " + sourceId);
        } catch (Exception e) {
            MsgUtils.printMsg("[雲端同步] PDF 上傳失敗: " + e.getMessage());
        }
    }

    public void uploadRealtimeInventoryIfEnabled() {
        try {
            if (!settings.getBoolean("drive.enabled") || !settings.getBoolean("drive.upload.inventoryRealtime") || !googleDriveService.isReady()) return;
            Path export = Path.of("./app/files/export/realtime_inventory.xlsx");
            FilesUtils.createFolder(export.getParent().toString());
            writeInventoryXlsx(export, inventoryService.getRealtimeInventory(), "即時庫存");
            var result = googleDriveService.uploadOrUpdate("即時庫存", export.toString(), settings.get("drive.inventoryRealtimeFileId"), "realtime_inventory.xlsx");
            settings.set("drive.inventoryRealtimeFileId", result.fileId());
            settings.set("drive.inventoryRealtimeUploadedAt", result.uploadedAt().toString());
            MsgUtils.printMsg("[雲端同步] 即時庫存 Excel 已更新");
        } catch (Exception e) {
            MsgUtils.printMsg("[雲端同步] 即時庫存 Excel 上傳失敗: " + e.getMessage());
        }
    }

    public void uploadInventoryBySchedule() {
        uploadDailyInventoryIfEnabled(LocalDate.now());
    }

    public void uploadDailyInventoryIfEnabled(LocalDate date) {
        try {
            if (!settings.getBoolean("drive.enabled") || !settings.getBoolean("drive.upload.dailyInventory") || !googleDriveService.isReady()) return;
            String y = String.valueOf(date.getYear());
            String m = String.format("%02d", date.getMonthValue());
            String fileName = date.format(DateTimeFormatter.ISO_DATE) + "_inventory.xlsx";
            Path export = Path.of("./app/files/export/daily", y, m, fileName);
            FilesUtils.createFolder(export.getParent().toString());
            writeInventoryXlsx(export, inventoryService.getRealtimeInventory(), "每日庫存表");
            googleDriveService.uploadOrUpdate("每日庫存表/" + y + "/" + m, export.toString(), "", fileName);
            settings.set("drive.dailyInventoryLastUploadedDate", date.toString());
            settings.set("drive.dailyInventoryLastUploadedAt", LocalDateTime.now().toString());
            MsgUtils.printMsg("[雲端同步] 每日庫存表已上傳: " + fileName);
        } catch (Exception e) {
            MsgUtils.printMsg("[雲端同步] 每日庫存表上傳失敗: " + e.getMessage());
        }
    }

    private void writeInventoryXlsx(Path path, List<Map<String, Object>> rows, String sheetName) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            String[] headers = {"品號", "類別", "品名", "小名", "規格", "顏色", "單位", "預設進貨價", "預設銷貨價", "備註", "產品狀態", "庫存", "安全庫存", "狀況"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row excel = sheet.createRow(r++);
                Object[] values = {row.get("productCode"), row.get("category"), row.get("productName"), row.get("productAlias"), row.get("specification"), row.get("color"), row.get("unit"), row.get("defaultPurchasePrice"), row.get("defaultSalePrice"), row.get("note"), row.get("activeText"), row.get("quantity"), row.get("safetyStock"), row.get("statusText")};
                for (int i = 0; i < values.length; i++) excel.createCell(i).setCellValue(values[i] == null ? "" : String.valueOf(values[i]));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                workbook.write(out);
            }
        }
    }

    private String safeFileName(String value) {
        return value == null ? "unknown" : value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
