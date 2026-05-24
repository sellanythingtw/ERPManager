package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LabelPrintSettingRepository extends JpaRepository<LabelPrintSetting, Long> {
    List<LabelPrintSetting> findAllByOrderByDefaultTemplateDescSettingIdAsc();

    /**
     * 可用範本：active = true 或舊資料 active 為 null。
     * 上版新增 active 欄位後，既有 SQLite 資料列可能是 null；若只查 active=true，
     * 進貨單新增/編輯頁會沒有任何貼紙範本可選。
     */
    @Query("""
            select s from LabelPrintSetting s
            where s.active = true or s.active is null
            order by s.defaultTemplate desc, s.settingId asc
            """)
    List<LabelPrintSetting> findUsableTemplates();

    @Query("""
            select s from LabelPrintSetting s
            where (s.active = true or s.active is null)
              and s.defaultTemplate = true
            order by s.settingId asc
            """)
    Optional<LabelPrintSetting> findFirstUsableDefaultTemplate();

    Optional<LabelPrintSetting> findFirstByDefaultTemplateTrueOrderBySettingIdAsc();
}
