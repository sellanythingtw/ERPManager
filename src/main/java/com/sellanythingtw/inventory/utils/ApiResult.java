package com.sellanythingtw.inventory.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApiResult {
    public static Map<String, Object> ok(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message == null ? "OK" : message);
        return result;
    }

    public static Map<String, Object> ok(String message, String key, Object value) {
        Map<String, Object> result = ok(message);
        result.put(key, value);
        return result;
    }

    public static Map<String, Object> fail(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message == null ? "執行失敗" : message);
        return result;
    }
}
