package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
