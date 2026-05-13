package com.sellanythingtw.inventory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地 SQLite 資料庫設定。
 *
 * 目的：
 * 1. 避免 IDE 或打包環境未正確載入 application.yml 時，Spring Boot 無法判斷 DataSource。
 * 2. 固定使用本地 SQLite 作為正式資料來源。
 * 3. 啟動時自動建立 app/data 資料夾。
 */
@Configuration
public class LocalDataSourceConfig {

    @Value("${spring.datasource.url:jdbc:sqlite:./app/data/app.db}")
    private String datasourceUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        tryCreateSqliteFolder(datasourceUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setPoolName("ERPManagerSQLitePool");

        // SQLite 建議關閉自動測試 query；Hikari 會透過 driver 判斷連線有效性。
        config.addDataSourceProperty("foreign_keys", "true");

        return new HikariDataSource(config);
    }

    private void tryCreateSqliteFolder(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:sqlite:")) {
            return;
        }

        String dbPathText = jdbcUrl.substring("jdbc:sqlite:".length());
        if (dbPathText.isBlank() || ":memory:".equals(dbPathText)) {
            return;
        }

        try {
            Path dbPath = Paths.get(dbPathText).toAbsolutePath().normalize();
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception ignored) {
            // 若建立資料夾失敗，讓 DataSource 在連線時回報正式錯誤。
        }
    }
}
