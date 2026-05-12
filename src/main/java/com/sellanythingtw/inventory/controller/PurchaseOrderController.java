package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.SupplierRepository;
import com.sellanythingtw.inventory.service.LabelPrintService;
import com.sellanythingtw.inventory.service.PurchaseOrderService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;
    private final LabelPrintService labelPrintService;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                   LabelPrintService labelPrintService,
                                   SupplierRepository supplierRepository,
                                   ProductRepository productRepository) {
        this.purchaseOrderService = purchaseOrderService;
        this.labelPrintService = labelPrintService;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/purchases/new")
    public String newPurchase(Model model) {
        model.addAttribute("suppliers", supplierRepository.findAll());
        return "purchase/new";
    }

    @GetMapping("/purchases/{purchaseId}")
    public String detail(@PathVariable Long purchaseId, Model model) {
        Map<String, Object> detail = purchaseOrderService.getDetail(purchaseId);
        model.addAllAttributes(detail);
        model.addAttribute("products", productRepository.findAll());
        return "purchase/detail";
    }

    @PostMapping("/purchases/draft")
    public String createDraftPage(@RequestParam(required = false) Long supplierId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        return "redirect:/purchases/" + purchaseOrderService.createDraft(supplierId, purchaseDate).getPurchaseId();
    }

    @PostMapping("/purchases/{purchaseId}/items")
    public String addItemPage(@PathVariable Long purchaseId,
                              @RequestParam Long productId,
                              @RequestParam(defaultValue = "0") BigDecimal wholesalePrice,
                              @RequestParam(defaultValue = "0") BigDecimal salePrice,
                              @RequestParam(required = false) String trayQuantityCode,
                              @RequestParam(required = false) String sizeCode,
                              @RequestParam Integer quantity) {
        purchaseOrderService.addItem(purchaseId, productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity);
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/purchases/{purchaseId}/items/{itemId}/delete")
    public String deleteItemPage(@PathVariable Long purchaseId, @PathVariable Long itemId) {
        purchaseOrderService.deleteItem(purchaseId, itemId);
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/purchases/{purchaseId}/confirm")
    public String confirmPage(@PathVariable Long purchaseId) {
        purchaseOrderService.confirm(purchaseId);
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/labels/lots/{lotId}")
    public String createLabelPage(@PathVariable Long lotId, @RequestParam Long purchaseId) {
        labelPrintService.createLotLabelPdf(lotId);
        return "redirect:/purchases/" + purchaseId + "?label=1";
    }

    @PostMapping("/labels/purchases/{purchaseId}/batch")
    public String createPurchaseLabelsPage(@PathVariable Long purchaseId,
                                           @RequestParam(defaultValue = "1") Integer copies) {
        labelPrintService.createPurchaseLabelsPdf(purchaseId, copies);
        return "redirect:/purchases/" + purchaseId + "?labels=1";
    }

    @PostMapping("/api/purchases/draft")
    @ResponseBody
    public Map<String, Object> createDraft(@RequestParam(required = false) Long supplierId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        return ApiResult.ok("進貨草稿已建立", "data", purchaseOrderService.createDraft(supplierId, purchaseDate));
    }

    @PostMapping("/api/purchases/{purchaseId}/items")
    @ResponseBody
    public Map<String, Object> addItem(@PathVariable Long purchaseId,
                                       @RequestParam Long productId,
                                       @RequestParam(defaultValue = "0") BigDecimal wholesalePrice,
                                       @RequestParam(defaultValue = "0") BigDecimal salePrice,
                                       @RequestParam(required = false) String trayQuantityCode,
                                       @RequestParam(required = false) String sizeCode,
                                       @RequestParam Integer quantity) {
        return ApiResult.ok("進貨明細已加入", "data", purchaseOrderService.addItem(purchaseId, productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity));
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

    @PostMapping("/api/labels/purchases/{purchaseId}/batch")
    @ResponseBody
    public Map<String, Object> createPurchaseLabels(@PathVariable Long purchaseId,
                                                    @RequestParam(defaultValue = "1") Integer copies) {
        return ApiResult.ok("整張進貨單貼紙 PDF 已產生", "path", labelPrintService.createPurchaseLabelsPdf(purchaseId, copies));
    }

}
