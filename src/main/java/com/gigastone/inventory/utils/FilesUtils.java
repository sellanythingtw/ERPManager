package com.gigastone.inventory.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FilesUtils {

    public static boolean createFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists()) return folder.mkdirs();
        return true;
    }

    public static void copyFile(String sourcePath, String destPath, boolean autoCreateFolder) throws IOException {
        Path source = Path.of(sourcePath);
        Path destination = Path.of(destPath);
        if (autoCreateFolder && destination.getParent() != null) Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String safeFileName(String text) {
        if (text == null || text.isBlank()) return "未命名";
        return text.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
