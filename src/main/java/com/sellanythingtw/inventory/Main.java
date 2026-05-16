package com.sellanythingtw.inventory;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.sellanythingtw.inventory.utils.MsgUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class Main {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MsgUtils.printMsg();
            MsgUtils.printMsg("關閉程序中...");
            MsgUtils.printMsg("所有模組已關閉！");
            MsgUtils.printMsg("【程式已關閉，可關閉視窗離開】");
        }));

        MsgUtils.printMsg();
        MsgUtils.printMsg("歡迎來到 ERPManager 本地端進銷存系統！");
        MsgUtils.printMsg("程式名稱: ERPManager, 程式版本: 0.3.8");
        MsgUtils.printMsg("系統模式: 本地 Java + SQLite");
        MsgUtils.printMsg();

        FilesUtils.createFolder("./app/data");
        FilesUtils.createFolder("./app/files/pdf/purchase");
        FilesUtils.createFolder("./app/files/pdf/sales");
        FilesUtils.createFolder("./app/files/pdf/receivable");
        FilesUtils.createFolder("./app/files/labels/purchase");
        FilesUtils.createFolder("./app/files/export/inventory");
        FilesUtils.createFolder("./app/files/export/payment");
        FilesUtils.createFolder("./app/files/backup");
        FilesUtils.createFolder("./app/google/tokens");

        SpringApplication.run(Main.class, args);

        MsgUtils.printMsg();
        MsgUtils.printMsg("系統已啟動，請使用瀏覽器開啟：http://127.0.0.1:8080");
        MsgUtils.printMsg();
    }
}
