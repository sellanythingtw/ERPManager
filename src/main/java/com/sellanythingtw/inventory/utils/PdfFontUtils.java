package com.sellanythingtw.inventory.utils;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PdfFontUtils {
    private static final Logger log = LoggerFactory.getLogger(PdfFontUtils.class);
    private static BaseFont cachedBaseFont;
    private static String cachedFontPath;

    private PdfFontUtils() {}

    public static BaseFont baseFont() {
        if (cachedBaseFont != null) return cachedBaseFont;

        String explicit = System.getProperty("erpmanager.pdf.font");
        if (explicit != null && !explicit.isBlank()) {
            BaseFont font = tryLoad(explicit.trim());
            if (font != null) return font;
        }

        List<String> candidates = List.of(
                // macOS common CJK fonts
                "/System/Library/Fonts/PingFang.ttc,0",
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Songti.ttc,0",
                "/System/Library/Fonts/STHeiti Light.ttc,0",
                "/System/Library/Fonts/STHeiti Medium.ttc,0",
                "/Library/Fonts/Arial Unicode.ttf",
                "/Library/Fonts/NotoSansTC-Regular.ttf",
                "/Library/Fonts/NotoSansCJKtc-Regular.otf",
                // Windows common CJK fonts
                "C:/Windows/Fonts/msjh.ttc,0",
                "C:/Windows/Fonts/msjh.ttf",
                "C:/Windows/Fonts/mingliu.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                // Linux common CJK fonts
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/opentype/noto/NotoSansCJKtc-Regular.otf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/truetype/noto/NotoSansTC-Regular.otf"
        );
        for (String candidate : candidates) {
            BaseFont font = tryLoad(candidate);
            if (font != null) return font;
        }
        try {
            cachedBaseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            cachedFontPath = "STSong-Light";
            log.warn("PDF 中文字型未找到系統字型，改用 OpenPDF fallback: {}", cachedFontPath);
            return cachedBaseFont;
        } catch (Exception ignored) {
        }
        try {
            cachedBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            cachedFontPath = BaseFont.HELVETICA;
            log.warn("PDF 中文字型載入失敗，改用 Helvetica；中文字可能無法顯示。請安裝 Noto Sans TC 或指定 -Derpmanager.pdf.font=/path/font.ttf");
            return cachedBaseFont;
        } catch (Exception ex) {
            throw new RuntimeException("無法載入 PDF 字型", ex);
        }
    }

    private static BaseFont tryLoad(String candidate) {
        String filePath = candidate.contains(",") ? candidate.substring(0, candidate.indexOf(',')) : candidate;
        if (!Files.exists(Path.of(filePath))) return null;
        try {
            cachedBaseFont = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            cachedFontPath = candidate;
            log.info("PDF 字型已載入：{}", cachedFontPath);
            return cachedBaseFont;
        } catch (Exception ex) {
            log.debug("PDF 字型載入失敗：{}，原因：{}", candidate, ex.getMessage());
            return null;
        }
    }

    public static String currentFontPath() {
        baseFont();
        return cachedFontPath;
    }

    public static Font font(int size, int style) {
        return new Font(baseFont(), size, style);
    }
}
