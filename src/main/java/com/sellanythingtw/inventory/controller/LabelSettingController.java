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
    public String labelSettings(@RequestParam(required = false) Long settingId, Model model) {
        var templates = labelPrintService.listTemplates();
        LabelPrintSetting setting = settingId == null ? new LabelPrintSetting() : labelPrintService.getTemplate(settingId);
        if (settingId == null && !templates.isEmpty()) {
            setting.setDefaultTemplate(false);
        }
        model.addAttribute("templates", templates);
        model.addAttribute("setting", setting);
        model.addAttribute("editingExisting", settingId != null);
        return "settings/labels";
    }

    @PostMapping("/settings/labels/new")
    public String createTemplate(RedirectAttributes redirectAttributes) {
        LabelPrintSetting setting = labelPrintService.createTemplate();
        redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已建立。");
        return "redirect:/settings/label-templates?settingId=" + setting.getSettingId();
    }

    @PostMapping("/settings/labels")
    public String saveLabelSettings(@ModelAttribute LabelPrintSetting setting, RedirectAttributes redirectAttributes) {
        LabelPrintSetting saved = labelPrintService.updateSetting(setting);
        redirectAttributes.addFlashAttribute("successMessage", "進貨貼紙範本已儲存。後續進貨單儲存時會依品項選用的範本自動更新貼紙 PDF。");
        return "redirect:/settings/label-templates?settingId=" + saved.getSettingId();
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
}
