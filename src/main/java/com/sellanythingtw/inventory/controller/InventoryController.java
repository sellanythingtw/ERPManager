package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.service.InventoryService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public String inventory(@RequestParam(required = false) String category,
                            @RequestParam(required = false) String productName,
                            @RequestParam(required = false) String color,
                            @RequestParam(required = false) String stockFilter,
                            @RequestParam(required = false) String statusFilter,
                            @RequestParam(required = false) String supplierName,
                            Model model) {
        var rows = inventoryService.searchRealtimeInventory(category, productName, color, stockFilter, statusFilter, supplierName);
        model.addAttribute("rows", rows);
        model.addAttribute("summary", inventoryService.inventorySummary(rows));
        model.addAttribute("category", category);
        model.addAttribute("productName", productName);
        model.addAttribute("color", color);
        model.addAttribute("stockFilter", stockFilter);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("supplierName", supplierName);
        return "inventory/list";
    }

    @GetMapping("/inventory/products/{productId}/lots")
    public String productLots(@PathVariable Long productId, Model model) {
        model.addAttribute("product", inventoryService.getProduct(productId));
        model.addAttribute("rows", inventoryService.getOpenLots(productId));
        return "inventory/lots";
    }

    @GetMapping("/inventory/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String productName,
                                            @RequestParam(required = false) String color,
                                            @RequestParam(required = false) String stockFilter,
                                            @RequestParam(required = false) String statusFilter,
                                            @RequestParam(required = false) String supplierName) {
        String csv = inventoryService.toCsv(inventoryService.searchRealtimeInventory(category, productName, color, stockFilter, statusFilter, supplierName));
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    @GetMapping("/api/inventory")
    @ResponseBody
    public Map<String, Object> apiInventory(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String productName,
                                            @RequestParam(required = false) String color,
                                            @RequestParam(required = false) String stockFilter,
                                            @RequestParam(required = false) String statusFilter,
                                            @RequestParam(required = false) String supplierName) {
        return ApiResult.ok("OK", "data", inventoryService.searchRealtimeInventory(category, productName, color, stockFilter, statusFilter, supplierName));
    }
}
