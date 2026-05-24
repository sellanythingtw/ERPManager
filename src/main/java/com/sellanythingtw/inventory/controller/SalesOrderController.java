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
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String customerCode,
                       @RequestParam(required = false) String customerName,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                       Model model) {
        model.addAttribute("rows", salesOrderService.searchRows(keyword, customerCode, customerName, status, dateFrom, dateTo));
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("customerCode", customerCode);
        model.addAttribute("customerName", customerName);
        model.addAttribute("status", status);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "sales/list";
    }

    @GetMapping("/sales/new")
    public String newSales(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "sales/new";
    }

    @GetMapping("/sales/{salesId}")
    public String detail(@PathVariable Long salesId, @RequestParam(defaultValue = "false") boolean edit, Model model) {
        model.addAllAttributes(salesOrderService.getDetail(salesId));
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("editMode", edit);
        return "sales/detail";
    }

    @PostMapping("/sales/create-full")
    public String createFullPage(@RequestParam(required = false) Long customerId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
                                 @RequestParam(required = false) String barcodeValue,
                                 @RequestParam(required = false) Integer quantity,
                                 @RequestParam(required = false) String itemNote,
                                 @RequestParam(defaultValue = "CASH") String paymentType,
                                 @RequestParam(defaultValue = "PAID") String paymentStatus,
                                 @RequestParam(defaultValue = "draft") String action,
                                 RedirectAttributes redirectAttributes) {
        try {
            boolean confirm = "confirm".equals(action);
            Long id = salesOrderService.createWithFirstItem(customerId, salesDate, barcodeValue, quantity, itemNote, paymentType, paymentStatus, confirm).getSalesId();
            redirectAttributes.addFlashAttribute("successMessage", confirm ? "銷貨單已建立並確認。" : "銷貨單草稿已建立。仍可由列表進入編輯或確認。");
            return "redirect:/sales/" + id + (confirm ? "" : "?edit=true");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sales/new";
        }
    }

    @PostMapping("/sales/draft")
    public String createDraftPage(@RequestParam(required = false) Long customerId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
                                  RedirectAttributes redirectAttributes) {
        try {
            Long id = salesOrderService.createDraft(customerId, salesDate).getSalesId();
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單草稿已建立，可先存放於列表，之後再回來編輯或確認。");
            return "redirect:/sales/" + id + "?edit=true";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sales/new";
        }
    }

    @PostMapping("/sales/{salesId}/header")
    public String updateHeaderPage(@PathVariable Long salesId,
                                   @RequestParam(required = false) Long customerId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
                                   RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.updateHeader(salesId, customerId, salesDate);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單已儲存。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sales/" + salesId + "?edit=true";
    }

    @PostMapping("/sales/{salesId}/items/barcode")
    public String addItemByBarcodePage(@PathVariable Long salesId,
                                       @RequestParam String barcodeValue,
                                       @RequestParam int quantity,
                                       @RequestParam(required = false) String itemNote,
                                       RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.addItemByBarcode(salesId, barcodeValue, quantity, itemNote);
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
                              @RequestParam(required = false) Long customerId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
                              @RequestParam(defaultValue = "CASH") String paymentType,
                              @RequestParam(defaultValue = "PAID") String paymentStatus,
                              RedirectAttributes redirectAttributes) {
        try {
            if (customerId != null || salesDate != null) {
                salesOrderService.updateHeader(salesId, customerId, salesDate);
            }
            salesOrderService.confirm(salesId, paymentType, paymentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單已確認，庫存已扣除。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sales/" + salesId + "?edit=true";
        }
        return "redirect:/sales/" + salesId;
    }

    @PostMapping("/sales/{salesId}/delete-draft")
    public String deleteDraftPage(@PathVariable Long salesId, RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.deleteDraft(salesId);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨草稿已刪除。");
            return "redirect:/sales";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sales/" + salesId;
        }
    }

    @PostMapping("/sales/{salesId}/void")
    public String voidPage(@PathVariable Long salesId, RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.voidOrder(salesId);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單已作廢，可於單據頁恢復。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sales/" + salesId;
    }

    @PostMapping("/sales/{salesId}/restore")
    public String restorePage(@PathVariable Long salesId, RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.restoreOrder(salesId);
            redirectAttributes.addFlashAttribute("successMessage", "銷貨單已恢復。");
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
                                                @RequestParam int quantity,
                                                @RequestParam(required = false) String itemNote) {
        return ApiResult.ok("銷貨明細已加入", "data", salesOrderService.addItemByBarcode(salesId, barcodeValue, quantity, itemNote));
    }

    @PostMapping("/api/sales/{salesId}/confirm")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long salesId,
                                       @RequestParam(defaultValue = "CASH") String paymentType,
                                       @RequestParam(defaultValue = "PAID") String paymentStatus) {
        return ApiResult.ok("銷貨單已確認", "data", salesOrderService.confirm(salesId, paymentType, paymentStatus));
    }
}
