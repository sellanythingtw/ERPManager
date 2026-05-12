package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.LabelPrintSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelPrintSettingRepository extends JpaRepository<LabelPrintSetting, Long> {
}
