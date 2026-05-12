package com.gigastone.inventory.controller;

import com.gigastone.inventory.service.SalesOrderService;
import com.gigastone.inventory.utils.ApiResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class SalesOrderController {
    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @GetMapping("/sales/new")
    public String newSales() {
        return "sales/new";
    }

    @PostMapping("/api/sales/draft")
    @ResponseBody
    public Map<String, Object> createDraft(@RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) LocalDate salesDate) {
        return ApiResult.ok("銷貨草稿已建立", "data", salesOrderService.createDraft(customerId, salesDate));
    }

    @GetMapping("/api/sales/barcode/{barcodeValue}")
    @ResponseBody
    public Map<String, Object> lookupBarcode(@PathVariable String barcodeValue) {
        return ApiResult.ok("OK", "data", salesOrderService.lookupBarcode(barcodeValue));
    }

    @PostMapping("/api/sales/{salesId}/items/barcode")
    @ResponseBody
    public Map<String, Object> addItemByBarcode(@PathVariable Long salesId,
                                                @RequestParam String barcodeValue,
                                                @RequestParam int quantity) {
        return ApiResult.ok("銷貨明細已加入", "data", salesOrderService.addItemByBarcode(salesId, barcodeValue, quantity));
    }

    @PostMapping("/api/sales/{salesId}/confirm")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long salesId, @RequestParam(defaultValue = "CASH") String paymentType) {
        return ApiResult.ok("銷貨單已確認", "data", salesOrderService.confirm(salesId, paymentType));
    }
}
