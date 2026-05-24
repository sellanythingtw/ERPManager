package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Controller
public class SettingsController {
    private final AppSettingService settings;
    private final GoogleDriveService googleDriveService;
    private final BackupService backupService;
    private final CloudSyncService cloudSyncService;

    public SettingsController(AppSettingService settings,
                              GoogleDriveService googleDriveService,
                              BackupService backupService,
                              CloudSyncService cloudSyncService) {
        this.settings = settings;
        this.googleDriveService = googleDriveService;
        this.backupService = backupService;
        this.cloudSyncService = cloudSyncService;
    }

    @GetMapping("/settings")
    public String settings(Model model, HttpServletRequest request) {
        model.addAttribute("settings", settings.allAsMap());
        model.addAttribute("driveStatus", googleDriveService.status());
        model.addAttribute("redirectUri", baseUrl(request) + "/settings/google/callback");
        return "settings/index";
    }

    @PostMapping("/settings/google/config")
    public String saveGoogleConfig(@RequestParam(required = false) String clientId,
                                   @RequestParam(required = false) String clientSecret,
                                   @RequestParam(required = false) String driveEnabled,
                                   @RequestParam(required = false) String purchasePdf,
                                   @RequestParam(required = false) String salesPdf,
                                   @RequestParam(required = false) String realtimeInventory,
                                   @RequestParam(required = false) String dailyInventory,
                                   @RequestParam(required = false) String dailyInventoryTime,
                                   RedirectAttributes redirectAttributes) {
        settings.set("google.clientId", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) settings.set("google.clientSecret", clientSecret);
        settings.setBoolean("drive.enabled", driveEnabled != null);
        settings.setBoolean("drive.upload.purchasePdf", purchasePdf != null);
        settings.setBoolean("drive.upload.salesPdf", salesPdf != null);
        settings.setBoolean("drive.upload.inventoryRealtime", realtimeInventory != null);
        settings.setBoolean("drive.upload.dailyInventory", dailyInventory != null);
        settings.set("drive.dailyInventoryTime", dailyInventoryTime == null || dailyInventoryTime.isBlank() ? "23:59" : dailyInventoryTime);
        redirectAttributes.addFlashAttribute("successMessage", "Google Drive 設定已儲存。");
        return "redirect:/settings";
    }

    @GetMapping("/settings/google/connect")
    public String connectGoogle(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            String url = googleDriveService.buildAuthorizationUrl(baseUrl(request) + "/settings/google/callback");
            return "redirect:" + url;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "建立 Google 授權連結失敗：" + e.getMessage());
            return "redirect:/settings";
        }
    }

    @GetMapping("/settings/google/callback")
    public String googleCallback(@RequestParam(required = false) String code,
                                 @RequestParam(required = false) String error,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Google Drive 授權取消或失敗：" + error);
            return "redirect:/settings";
        }
        try {
            googleDriveService.handleCallback(code, baseUrl(request) + "/settings/google/callback");
            redirectAttributes.addFlashAttribute("successMessage", "Google Drive 已連接。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Google Drive 連接失敗：" + e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/google/disconnect")
    public String disconnectGoogle(RedirectAttributes redirectAttributes) {
        try {
            googleDriveService.disconnect();
            redirectAttributes.addFlashAttribute("successMessage", "Google Drive 已解除連接。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "解除連接失敗：" + e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/google/test-inventory")
    public String testInventoryUpload(RedirectAttributes redirectAttributes) {
        try {
            cloudSyncService.uploadRealtimeInventoryIfEnabled();
            redirectAttributes.addFlashAttribute("successMessage", "已執行即時庫存上傳測試；請檢查 Google Drive 或 console log。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "測試上傳失敗：" + e.getMessage());
        }
        return "redirect:/settings";
    }

    @GetMapping("/settings/backup/export")
    public ResponseEntity<byte[]> exportBackup() {
        try {
            Path backup = backupService.createFullBackup();
            byte[] bytes = Files.readAllBytes(backup);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backup.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("備份匯出失敗：" + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/settings/backup/import")
    public String importBackup(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            Path imported = backupService.importBackup(file);
            redirectAttributes.addFlashAttribute("successMessage", "備份已匯入：" + imported.getFileName() + "。若資料庫檔案有變更，請重新啟動系統。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "備份匯入失敗：" + e.getMessage());
        }
        return "redirect:/settings";
    }

    private String baseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean standard = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + host + (standard ? "" : ":" + port);
    }
}
