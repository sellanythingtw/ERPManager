package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelPrintSettingRepository extends JpaRepository<LabelPrintSetting, Long> {
    List<LabelPrintSetting> findAllByOrderByDefaultTemplateDescSettingIdAsc();
    Optional<LabelPrintSetting> findFirstByDefaultTemplateTrueOrderBySettingIdAsc();
}
