package com.sellanythingtw.inventory.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;
import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.utils.FilesUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {
    private static final String USER_ID = "default";
    private static final String ROOT_FOLDER = "ERPManager";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final AppProperties appProperties;
    private final AppSettingService settings;

    public GoogleDriveService(AppProperties appProperties, AppSettingService settings) {
        this.appProperties = appProperties;
        this.settings = settings;
    }

    public DriveStatus status() {
        boolean configured = hasText(settings.get("google.clientId")) && hasText(settings.get("google.clientSecret"));
        boolean connected = false;
        String account = settings.get("google.accountEmail");
        try {
            connected = configured && loadCredential() != null;
        } catch (Exception ignored) {
            connected = false;
        }
        return new DriveStatus(configured, connected, account, settings.getBoolean("drive.enabled"));
    }

    public String buildAuthorizationUrl(String redirectUri) throws Exception {
        return flow().newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setAccessType("offline")
                .set("prompt", "consent")
                .build();
    }

    public void handleCallback(String code, String redirectUri) throws Exception {
        GoogleAuthorizationCodeFlow flow = flow();
        var token = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        flow.createAndStoreCredential(token, USER_ID);
        try {
            Drive drive = drive();
            About about = drive.about().get().setFields("user(emailAddress,displayName)").execute();
            if (about.getUser() != null) {
                settings.set("google.accountEmail", nvl(about.getUser().getEmailAddress(), about.getUser().getDisplayName()));
            }
        } catch (Exception e) {
            settings.set("google.accountEmail", "已連接");
        }
    }

    public void disconnect() throws IOException {
        deleteRecursively(tokenDir());
        settings.set("google.accountEmail", "");
    }

    public boolean isReady() {
        try {
            return settings.getBoolean("drive.enabled") && loadCredential() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public DriveUploadResult uploadOrUpdate(String folderPath, String localFilePath, String existingFileId, String fixedFileName) throws Exception {
        if (!isReady()) throw new IllegalStateException("Google Drive 尚未連接或未啟用");
        Path path = Path.of(localFilePath);
        if (!Files.exists(path)) throw new IllegalArgumentException("找不到本機檔案：" + localFilePath);

        Drive drive = drive();
        String folderId = ensureFolderPath(drive, folderPath);
        String fileName = hasText(fixedFileName) ? fixedFileName : path.getFileName().toString();
        String mimeType = Files.probeContentType(path);
        if (!hasText(mimeType)) mimeType = guessMimeType(fileName);

        com.google.api.services.drive.model.File metadata = new com.google.api.services.drive.model.File();
        metadata.setName(fileName);
        metadata.setParents(Collections.singletonList(folderId));
        FileContent media = new FileContent(mimeType, path.toFile());

        String fileId = existingFileId;
        if (!hasText(fileId) || !existsFile(drive, fileId)) {
            fileId = findFileInFolder(drive, folderId, fileName);
        }

        com.google.api.services.drive.model.File uploaded;
        if (hasText(fileId)) {
            uploaded = drive.files().update(fileId, metadata, media)
                    .setFields("id,name,parents")
                    .execute();
        } else {
            uploaded = drive.files().create(metadata, media)
                    .setFields("id,name,parents")
                    .execute();
        }
        return new DriveUploadResult(uploaded.getId(), uploaded.getName(), folderId, LocalDateTime.now());
    }

    private Drive drive() throws Exception {
        Credential credential = loadCredential();
        if (credential == null) throw new IllegalStateException("Google Drive 尚未連接");
        return new Drive.Builder(httpTransport(), JSON_FACTORY, credential)
                .setApplicationName("ERPManager")
                .build();
    }

    private Credential loadCredential() throws Exception {
        return flow().loadCredential(USER_ID);
    }

    private GoogleAuthorizationCodeFlow flow() throws Exception {
        String clientId = settings.get("google.clientId");
        String clientSecret = settings.get("google.clientSecret");
        if (!hasText(clientId) || !hasText(clientSecret)) throw new IllegalStateException("請先設定 Google OAuth Client ID / Client Secret");

        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(details);

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport(),
                JSON_FACTORY,
                clientSecrets,
                List.of(DriveScopes.DRIVE_FILE)
        )
                .setDataStoreFactory(new FileDataStoreFactory(tokenDir().toFile()))
                .setAccessType("offline")
                .build();
    }

    private HttpTransport httpTransport() throws Exception {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    private Path tokenDir() {
        Path path = Path.of(nvl(appProperties.getGoogleTokenDir(), "./app/google/tokens"));
        FilesUtils.createFolder(path.toString());
        return path;
    }

    private String ensureFolderPath(Drive drive, String folderPath) throws IOException {
        String parent = ensureFolder(drive, null, ROOT_FOLDER);
        if (!hasText(folderPath)) return parent;
        List<String> parts = new ArrayList<>();
        for (String part : folderPath.split("/")) {
            if (hasText(part)) parts.add(part.trim());
        }
        for (String part : parts) {
            parent = ensureFolder(drive, parent, part);
        }
        return parent;
    }

    private String ensureFolder(Drive drive, String parentId, String name) throws IOException {
        String existing = findFolder(drive, parentId, name);
        if (hasText(existing)) return existing;
        com.google.api.services.drive.model.File folder = new com.google.api.services.drive.model.File();
        folder.setName(name);
        folder.setMimeType("application/vnd.google-apps.folder");
        if (hasText(parentId)) folder.setParents(Collections.singletonList(parentId));
        return drive.files().create(folder).setFields("id").execute().getId();
    }

    private String findFolder(Drive drive, String parentId, String name) throws IOException {
        String q = "mimeType = 'application/vnd.google-apps.folder' and trashed = false and name = '" + escapeQuery(name) + "'";
        if (hasText(parentId)) q += " and '" + parentId + "' in parents";
        FileList list = drive.files().list().setQ(q).setFields("files(id,name)").setPageSize(1).execute();
        return list.getFiles() == null || list.getFiles().isEmpty() ? "" : list.getFiles().get(0).getId();
    }

    private String findFileInFolder(Drive drive, String folderId, String name) throws IOException {
        String q = "trashed = false and name = '" + escapeQuery(name) + "' and '" + folderId + "' in parents";
        FileList list = drive.files().list().setQ(q).setFields("files(id,name)").setPageSize(1).execute();
        return list.getFiles() == null || list.getFiles().isEmpty() ? "" : list.getFiles().get(0).getId();
    }

    private boolean existsFile(Drive drive, String fileId) {
        try {
            drive.files().get(fileId).setFields("id").execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String guessMimeType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }

    private String escapeQuery(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String nvl(String a, String b) { return hasText(a) ? a : b; }

    public record DriveStatus(boolean configured, boolean connected, String accountEmail, boolean enabled) {}
    public record DriveUploadResult(String fileId, String fileName, String folderId, LocalDateTime uploadedAt) {}
}
