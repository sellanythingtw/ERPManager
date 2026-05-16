package com.sellanythingtw.inventory.utils;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PdfFontUtils {
    private static BaseFont cachedBaseFont;

    private PdfFontUtils() {}

    public static BaseFont baseFont() {
        if (cachedBaseFont != null) return cachedBaseFont;
        List<String> candidates = List.of(
                // macOS
                "/System/Library/Fonts/PingFang.ttc,0",
                "/System/Library/Fonts/STHeiti Light.ttc,0",
                "/System/Library/Fonts/STHeiti Medium.ttc,0",
                "/Library/Fonts/Arial Unicode.ttf",
                "/Library/Fonts/NotoSansTC-Regular.ttf",
                // Windows
                "C:/Windows/Fonts/msjh.ttc,0",
                "C:/Windows/Fonts/msjhbd.ttc,0",
                "C:/Windows/Fonts/mingliu.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                // Linux common
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/truetype/noto/NotoSansTC-Regular.otf",
                "/usr/share/fonts/opentype/noto/NotoSansTC-Regular.otf"
        );
        for (String candidate : candidates) {
            String filePath = candidate.contains(",") ? candidate.substring(0, candidate.indexOf(',')) : candidate;
            if (!Files.exists(Path.of(filePath))) continue;
            try {
                cachedBaseFont = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return cachedBaseFont;
            } catch (Exception ignored) {
            }
        }
        try {
            cachedBaseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return cachedBaseFont;
        } catch (Exception ignored) {
        }
        try {
            cachedBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            return cachedBaseFont;
        } catch (Exception ex) {
            throw new RuntimeException("無法載入 PDF 字型", ex);
        }
    }

    public static Font font(int size, int style) {
        return new Font(baseFont(), size, style);
    }
}
