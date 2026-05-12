package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        Path base = Path.of(appProperties.getBaseDir()).toAbsolutePath().normalize();
        Path target = Path.of(path).toAbsolutePath().normalize();
        if (!target.startsWith(base)) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.exists(target) || Files.isDirectory(target)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(target.toUri());
        String filename = target.getFileName().toString();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
