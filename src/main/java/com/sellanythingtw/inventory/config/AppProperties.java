package com.sellanythingtw.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory")
public class AppProperties {
    private String appName;
    private String version;
    private String baseDir;
    private String pdfDir;
    private String labelDir;
    private String exportDir;
    private String backupDir;
    private String googleTokenDir;

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getBaseDir() { return baseDir; }
    public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
    public String getPdfDir() { return pdfDir; }
    public void setPdfDir(String pdfDir) { this.pdfDir = pdfDir; }
    public String getLabelDir() { return labelDir; }
    public void setLabelDir(String labelDir) { this.labelDir = labelDir; }
    public String getExportDir() { return exportDir; }
    public void setExportDir(String exportDir) { this.exportDir = exportDir; }
    public String getBackupDir() { return backupDir; }
    public void setBackupDir(String backupDir) { this.backupDir = backupDir; }
    public String getGoogleTokenDir() { return googleTokenDir; }
    public void setGoogleTokenDir(String googleTokenDir) { this.googleTokenDir = googleTokenDir; }
}
