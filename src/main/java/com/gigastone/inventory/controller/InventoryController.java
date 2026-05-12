package com.gigastone.inventory.controller;

import com.gigastone.inventory.service.InventoryService;
import com.gigastone.inventory.utils.ApiResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("rows", inventoryService.getRealtimeInventory());
        return "inventory/list";
    }

    @GetMapping("/api/inventory")
    @ResponseBody
    public Map<String, Object> apiInventory() {
        return ApiResult.ok("OK", "data", inventoryService.getRealtimeInventory());
    }
}
