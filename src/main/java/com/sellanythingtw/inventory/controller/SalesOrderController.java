package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.service.SalesOrderService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/sales")
    public String list(Model model) {
        model.addAttribute("orders", salesOrderService.listAll());
        model.addAttribute("customers", customerRepository.findAll());
        return "sales/list";
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
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
                                  RedirectAttributes redirectAttributes) {
        try {
            Long id = salesOrderService.createDraft(customerId, salesDate).getSalesId();
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單草稿已建立，可先存放於列表，之後再回來編輯或確認。");
            return "redirect:/sales/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sales/new";
        }
    }

    @PostMapping("/sales/{salesId}/items/barcode")
    public String addItemByBarcodePage(@PathVariable Long salesId,
                                       @RequestParam String barcodeValue,
                                       @RequestParam int quantity,
                                       RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.addItemByBarcode(salesId, barcodeValue, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨明細已加入草稿。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sales/" + salesId;
    }

    @PostMapping("/sales/{salesId}/items/{itemId}/delete")
    public String deleteItemPage(@PathVariable Long salesId, @PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.deleteItem(salesId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨明細已刪除。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sales/" + salesId;
    }

    @PostMapping("/sales/{salesId}/confirm")
    public String confirmPage(@PathVariable Long salesId,
                              @RequestParam(defaultValue = "CASH") String paymentType,
                              RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.confirm(salesId, paymentType);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單已確認，庫存已扣除。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
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
