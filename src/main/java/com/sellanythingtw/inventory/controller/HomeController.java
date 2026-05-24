package com.sellanythingtw.inventory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "redirect:/products";
    }

    @GetMapping("/home")
    public String home() {
        return "index";
    }
}
