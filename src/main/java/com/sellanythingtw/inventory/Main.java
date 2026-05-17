package com.sellanythingtw.inventory;

import com.sellanythingtw.inventory.config.AppProperties;
import com.sellanythingtw.inventory.utils.FilesUtils;
import com.sellanythingtw.inventory.utils.MsgUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class Main {

    private static final String VERSION = "0.4.0";
    private static final String HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 18780;
    private static final int MAX_PORT = 18799;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MsgUtils.printMsg();
            MsgUtils.printMsg("關閉程序中...");
            MsgUtils.printMsg("所有模組已關閉！");
            MsgUtils.printMsg("【程式已關閉，可關閉視窗離開】");
        }));

        int selectedPort = resolvePort(args);
        System.setProperty("server.address", HOST);
        System.setProperty("server.port", String.valueOf(selectedPort));

        MsgUtils.printMsg();
        MsgUtils.printMsg("歡迎來到 ERPManager 本地端進銷存系統！");
        MsgUtils.printMsg("程式名稱: ERPManager, 程式版本: " + VERSION);
        MsgUtils.printMsg("系統模式: 本地 Java + SQLite");
        MsgUtils.printMsg("預設連線位置: http://" + HOST + ":" + selectedPort);
        MsgUtils.printMsg();

        createRequiredFolders();

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        int actualPort = selectedPort;
        if (context instanceof WebServerApplicationContext webContext) {
            actualPort = webContext.getWebServer().getPort();
        }

        MsgUtils.printMsg();
        MsgUtils.printMsg("系統已啟動，請使用瀏覽器開啟：http://" + HOST + ":" + actualPort);
        MsgUtils.printMsg();
    }

    private static void createRequiredFolders() {
        FilesUtils.createFolder("./app/data");
        FilesUtils.createFolder("./app/files/pdf/purchase");
        FilesUtils.createFolder("./app/files/pdf/sales");
        FilesUtils.createFolder("./app/files/pdf/receivable");
        FilesUtils.createFolder("./app/files/labels/purchase");
        FilesUtils.createFolder("./app/files/export/inventory");
        FilesUtils.createFolder("./app/files/export/payment");
        FilesUtils.createFolder("./app/files/backup");
        FilesUtils.createFolder("./app/google/tokens");
    }

    private static int resolvePort(String[] args) {
        Integer cliPort = readCommandLinePort(args);
        if (cliPort != null) {
            return cliPort;
        }

        String systemPort = System.getProperty("server.port");
        if (isInteger(systemPort)) {
            return Integer.parseInt(systemPort);
        }

        String envPort = System.getenv("ERP_MANAGER_PORT");
        if (isInteger(envPort)) {
            return Integer.parseInt(envPort);
        }

        for (int port = DEFAULT_PORT; port <= MAX_PORT; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }

        return DEFAULT_PORT;
    }

    private static Integer readCommandLinePort(String[] args) {
        if (args == null) {
            return null;
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith("--server.port=")) {
                String value = arg.substring("--server.port=".length()).trim();
                if (isInteger(value)) {
                    return Integer.parseInt(value);
                }
            }
        }
        return null;
    }

    private static boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            int port = Integer.parseInt(value.trim());
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(HOST, port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
