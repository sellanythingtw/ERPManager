package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.service.SalesOrderService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class SalesOrderController {
    private final SalesOrderService salesOrderService;
    private final CustomerRepository customerRepository;

    public SalesOrderController(SalesOrderService salesOrderService, CustomerRepository customerRepository) {
        this.salesOrderService = salesOrderService;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/sales/new")
    public String newSales(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        return "sales/new";
    }

    @GetMapping("/sales/{salesId}")
    public String detail(@PathVariable Long salesId, Model model) {
        model.addAllAttributes(salesOrderService.getDetail(salesId));
        return "sales/detail";
    }

    @PostMapping("/sales/draft")
    public String createDraftPage(@RequestParam(required = false) Long customerId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate) {
        return "redirect:/sales/" + salesOrderService.createDraft(customerId, salesDate).getSalesId();
    }

    @PostMapping("/sales/{salesId}/items/barcode")
    public String addItemByBarcodePage(@PathVariable Long salesId,
                                       @RequestParam String barcodeValue,
                                       @RequestParam int quantity) {
        salesOrderService.addItemByBarcode(salesId, barcodeValue, quantity);
        return "redirect:/sales/" + salesId;
    }

    @PostMapping("/sales/{salesId}/confirm")
    public String confirmPage(@PathVariable Long salesId, @RequestParam(defaultValue = "CASH") String paymentType) {
        salesOrderService.confirm(salesId, paymentType);
        return "redirect:/sales/" + salesId;
    }

    @PostMapping("/api/sales/draft")
    @ResponseBody
    public Map<String, Object> createDraft(@RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate) {
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
