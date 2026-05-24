package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.AppSetting;
import com.sellanythingtw.inventory.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AppSettingService {
    private final AppSettingRepository repository;

    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public String get(String key) {
        return repository.findById(key).map(AppSetting::getSettingValue).orElse("");
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public boolean getBoolean(String key) {
        String value = get(key);
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    @Transactional
    public void set(String key, String value) {
        AppSetting setting = repository.findById(key).orElseGet(AppSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value == null ? "" : value.trim());
        repository.save(setting);
    }

    @Transactional
    public void setBoolean(String key, boolean value) {
        set(key, value ? "true" : "false");
    }

    public Map<String, String> allAsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        repository.findAll().stream()
                .sorted((a, b) -> a.getSettingKey().compareToIgnoreCase(b.getSettingKey()))
                .forEach(s -> map.put(s.getSettingKey(), s.getSettingValue()));
        return map;
    }
}
