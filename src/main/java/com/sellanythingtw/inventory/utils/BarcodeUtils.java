package com.sellanythingtw.inventory.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

public class BarcodeUtils {
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    public static String createLotBarcode(LocalDate purchaseDate, long sequence) {
        String dateText = purchaseDate == null ? LocalDate.now().format(YYMMDD) : purchaseDate.format(YYMMDD);
        return dateText + String.format("%06d", sequence);
    }

    public static byte[] createCode128Png(String value, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.CODE_128, width, height, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "png", out);
        return out.toByteArray();
    }

    public static String createCode128Base64(String value, int width, int height) throws Exception {
        return Base64.getEncoder().encodeToString(createCode128Png(value, width, height));
    }

    public static void writeCode128Png(String value, int width, int height, Path path) throws Exception {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.write(path, createCode128Png(value, width, height));
    }
}
