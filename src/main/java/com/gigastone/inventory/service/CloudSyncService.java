package com.gigastone.inventory.service;

import com.gigastone.inventory.utils.MsgUtils;
import org.springframework.stereotype.Service;

@Service
public class CloudSyncService {

    public void uploadPdfIfEnabled(String sourceType, Long sourceId, String localFilePath, String folderName) {
        // v0.1.0：先保留整合點。
        // 後續實作 Google Drive OAuth、分類資料夾建立、檔案上傳與 cloud_sync_logs。
        MsgUtils.printMsg("[雲端同步] 暫未啟用，略過 PDF 上傳: " + sourceType + " / " + sourceId + " / " + localFilePath);
    }

    public void uploadInventoryBySchedule() {
        // v0.1.0：先保留排程整合點。
        MsgUtils.printMsg("[雲端同步] 暫未啟用，略過庫存表排程上傳");
    }
}
