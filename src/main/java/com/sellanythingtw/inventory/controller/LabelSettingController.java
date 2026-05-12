package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.service.LabelPrintService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LabelSettingController {
    private final LabelPrintService labelPrintService;

    public LabelSettingController(LabelPrintService labelPrintService) {
        this.labelPrintService = labelPrintService;
    }

    @GetMapping("/settings/labels")
    public String labelSettings(Model model) {
        model.addAttribute("setting", labelPrintService.getSetting());
        return "settings/labels";
    }

    @PostMapping("/settings/labels")
    public String saveLabelSettings(@ModelAttribute LabelPrintSetting setting) {
        labelPrintService.updateSetting(setting);
        return "redirect:/settings/labels?saved=1";
    }
}
