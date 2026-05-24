package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("summary", dashboardService.summary());
        return "index";
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("summary", dashboardService.summary());
        return "index";
    }
}
