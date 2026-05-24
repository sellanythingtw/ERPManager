package com.sellanythingtw.inventory.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class DailyInventoryScheduleService {
    private final AppSettingService settings;
    private final CloudSyncService cloudSyncService;

    public DailyInventoryScheduleService(AppSettingService settings, CloudSyncService cloudSyncService) {
        this.settings = settings;
        this.cloudSyncService = cloudSyncService;
    }

    @Scheduled(fixedDelay = 60000)
    public void runDailyInventoryUpload() {
        if (!settings.getBoolean("drive.upload.dailyInventory")) return;
        String configured = settings.get("drive.dailyInventoryTime", "23:59");
        LocalTime target;
        try {
            target = LocalTime.parse(configured);
        } catch (Exception e) {
            target = LocalTime.of(23, 59);
        }
        LocalTime now = LocalTime.now();
        if (now.getHour() != target.getHour() || now.getMinute() != target.getMinute()) return;
        LocalDate today = LocalDate.now();
        if (today.toString().equals(settings.get("drive.dailyInventoryLastUploadedDate"))) return;
        cloudSyncService.uploadDailyInventoryIfEnabled(today);
    }
}
