package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class FileViewController {
    private final AppProperties appProperties;

    public FileViewController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/local-file")
    public ResponseEntity<Resource> localFile(@RequestParam String path) throws Exception {
        return fileResponse(path, true);
    }

    @GetMapping("/local-file/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) throws Exception {
        return fileResponse(path, false);
    }

    @GetMapping("/local-file/print")
    public String printFile(@RequestParam String path, Model model) throws Exception {
        validatePath(path);
        model.addAttribute("path", path);
        return "print-file";
    }

    @GetMapping("/local-file/open-folder")
    public String openFolder(@RequestParam String path,
                             @RequestParam(required = false, defaultValue = "/") String back,
                             RedirectAttributes redirectAttributes) {
        try {
            Path target = validatePath(path);
            Path folder = Files.isDirectory(target) ? target : target.getParent();
            openFolderByOs(folder);
            redirectAttributes.addFlashAttribute("successMessage", "已嘗試開啟資料夾：" + folder);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "開啟資料夾失敗：" + ex.getMessage());
        }
        return "redirect:" + back;
    }

    private void openFolderByOs(Path folder) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            new ProcessBuilder("open", folder.toString()).start();
        } else if (os.contains("win")) {
            new ProcessBuilder("explorer.exe", folder.toString()).start();
        } else {
            new ProcessBuilder("xdg-open", folder.toString()).start();
        }
    }

    private ResponseEntity<Resource> fileResponse(String path, boolean inline) throws Exception {
        Path target = validatePath(path);
        Resource resource = new UrlResource(target.toUri());
        String filename = target.getFileName().toString();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = inline ? "inline" : "attachment";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    private Path validatePath(String path) throws Exception {
        Path base = Path.of(appProperties.getBaseDir()).toAbsolutePath().normalize();
        Path target = Path.of(path).toAbsolutePath().normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("不允許讀取此路徑");
        }
        if (!Files.exists(target) || Files.isDirectory(target)) {
            throw new IllegalArgumentException("檔案不存在");
        }
        return target;
    }
}
