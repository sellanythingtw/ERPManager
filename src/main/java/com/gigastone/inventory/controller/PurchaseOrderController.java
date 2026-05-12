package com.gigastone.inventory.controller;

import com.gigastone.inventory.service.LabelPrintService;
import com.gigastone.inventory.service.PurchaseOrderService;
import com.gigastone.inventory.utils.ApiResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;
    private final LabelPrintService labelPrintService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService, LabelPrintService labelPrintService) {
        this.purchaseOrderService = purchaseOrderService;
        this.labelPrintService = labelPrintService;
    }

    @GetMapping("/purchases/new")
    public String newPurchase() {
        return "purchase/new";
    }

    @PostMapping("/api/purchases/draft")
    @ResponseBody
    public Map<String, Object> createDraft(@RequestParam(required = false) Long supplierId,
                                           @RequestParam(required = false) LocalDate purchaseDate) {
        return ApiResult.ok("進貨草稿已建立", "data", purchaseOrderService.createDraft(supplierId, purchaseDate));
    }

    @PostMapping("/api/purchases/{purchaseId}/confirm")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long purchaseId) {
        return ApiResult.ok("進貨單已確認", "data", purchaseOrderService.confirm(purchaseId));
    }

    @GetMapping("/api/purchases/{purchaseId}/lots")
    @ResponseBody
    public Map<String, Object> lots(@PathVariable Long purchaseId) {
        return ApiResult.ok("OK", "data", purchaseOrderService.listLots(purchaseId));
    }

    @PostMapping("/api/labels/lots/{lotId}")
    @ResponseBody
    public Map<String, Object> createLabel(@PathVariable Long lotId) {
        return ApiResult.ok("標籤 PDF 已產生", "path", labelPrintService.createLotLabelPdf(lotId));
    }
}
