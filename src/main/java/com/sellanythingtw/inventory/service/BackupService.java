package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.utils.FilesUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final AppProperties appProperties;

    public BackupService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Path createFullBackup() throws IOException {
        Path backupDir = Path.of(nvl(appProperties.getBackupDir(), "./app/files/backup"));
        FilesUtils.createFolder(backupDir.toString());
        String fileName = "ERPManager_backup_" + LocalDateTime.now().format(TS) + ".zip";
        Path zip = backupDir.resolve(fileName);
        Path base = Path.of(nvl(appProperties.getBaseDir(), "./app")).normalize();
        if (!Files.exists(base)) Files.createDirectories(base);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            addText(zos, "manifest.txt", "ERPManager backup\ncreatedAt=" + LocalDateTime.now() + "\nversion=" + nvl(appProperties.getVersion(), "") + "\n");
            try (var stream = Files.walk(base)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> !p.normalize().startsWith(backupDir.normalize()))
                        .filter(p -> !p.normalize().toString().contains("google" + java.io.File.separator + "tokens"))
                        .forEach(p -> addFile(zos, base, p));
            }
        }
        return zip;
    }

    public Path importBackup(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("請選擇備份 ZIP 檔");
        Path backupDir = Path.of(nvl(appProperties.getBackupDir(), "./app/files/backup"));
        FilesUtils.createFolder(backupDir.toString());
        Path saved = backupDir.resolve("import_" + LocalDateTime.now().format(TS) + "_" + safeName(file.getOriginalFilename()));
        file.transferTo(saved);

        // 匯入前先備份目前資料，避免匯入失敗無法回復。
        createFullBackup();

        Path base = Path.of(nvl(appProperties.getBaseDir(), "./app")).normalize();
        Files.createDirectories(base);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(saved))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (entry.getName().startsWith("manifest")) continue;
                Path out = base.resolve(entry.getName()).normalize();
                if (!out.startsWith(base)) throw new IOException("備份檔包含不安全路徑：" + entry.getName());
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return saved;
    }

    private void addFile(ZipOutputStream zos, Path base, Path file) {
        try {
            ZipEntry entry = new ZipEntry(base.relativize(file).toString().replace('\\', '/'));
            zos.putNextEntry(entry);
            Files.copy(file, zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addText(ZipOutputStream zos, String name, String text) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "backup.zip";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String nvl(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
