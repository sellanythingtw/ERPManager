package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.service.LabelPrintService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LabelSettingController {
    private final LabelPrintService labelPrintService;

    public LabelSettingController(LabelPrintService labelPrintService) {
        this.labelPrintService = labelPrintService;
    }

    @GetMapping({"/settings/labels", "/settings/label-templates"})
    public String labelSettings(@RequestParam(required = false) Long settingId,
                                @RequestParam(required = false) String templateName,
                                @RequestParam(required = false) String activeStatus,
                                Model model) {
        var templates = labelPrintService.listAllTemplates().stream()
                .filter(t -> !hasText(templateName) || safe(t.getTemplateName()).contains(templateName.trim()))
                .filter(t -> {
                    if (!hasText(activeStatus)) return true;
                    boolean active = t.getActive() == null || Boolean.TRUE.equals(t.getActive());
                    return "ACTIVE".equals(activeStatus) ? active : !active;
                })
                .toList();
        LabelPrintSetting setting = settingId == null ? new LabelPrintSetting() : labelPrintService.getTemplate(settingId);
        if (settingId == null && !templates.isEmpty()) {
            setting.setDefaultTemplate(false);
        }
        model.addAttribute("templates", templates);
        model.addAttribute("setting", setting);
        model.addAttribute("editingExisting", settingId != null);
        model.addAttribute("templateName", templateName);
        model.addAttribute("activeStatus", activeStatus);
        return "settings/labels";
    }

    @PostMapping("/settings/labels/new")
    public String createTemplate(RedirectAttributes redirectAttributes) {
        LabelPrintSetting setting = labelPrintService.createTemplate();
        redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已建立。");
        return "redirect:/settings/label-templates";
    }

    @PostMapping("/settings/labels")
    public String saveLabelSettings(@ModelAttribute LabelPrintSetting setting, RedirectAttributes redirectAttributes) {
        labelPrintService.updateSetting(setting);
        redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已儲存。後續進貨單儲存時會依品項選用的範本自動更新貼紙 PDF。");
        return "redirect:/settings/label-templates";
    }

    @PostMapping("/settings/labels/{settingId}/delete")
    public String deleteTemplate(@PathVariable Long settingId, RedirectAttributes redirectAttributes) {
        try {
            labelPrintService.deleteTemplate(settingId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已刪除。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/label-templates";
    }

    @PostMapping("/settings/labels/{settingId}/void")
    public String voidTemplate(@PathVariable Long settingId, RedirectAttributes redirectAttributes) {
        try {
            labelPrintService.voidTemplate(settingId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已作廢，不會再出現在進貨單範本下拉選單。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/settings/label-templates?settingId=" + settingId;
        }
        return "redirect:/settings/label-templates";
    }

    @PostMapping("/settings/labels/{settingId}/restore")
    public String restoreTemplate(@PathVariable Long settingId, RedirectAttributes redirectAttributes) {
        try {
            labelPrintService.restoreTemplate(settingId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已恢復使用。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/label-templates?settingId=" + settingId;
    }

    @PostMapping("/settings/labels/{settingId}/test-pdf")
    public String createTestPdf(@PathVariable Long settingId,
                                @ModelAttribute LabelPrintSetting setting,
                                RedirectAttributes redirectAttributes) {
        try {
            setting.setSettingId(settingId);
            LabelPrintSetting saved = labelPrintService.updateSetting(setting);
            String path = labelPrintService.createTestLabelPdf(saved.getSettingId());
            redirectAttributes.addFlashAttribute("successMessage", "測試貼紙 PDF 已產生：" + path);
            redirectAttributes.addFlashAttribute("testPdfPath", path);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/label-templates?settingId=" + settingId;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

}
