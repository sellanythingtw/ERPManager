package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelPrintSettingRepository extends JpaRepository<LabelPrintSetting, Long> {
}
