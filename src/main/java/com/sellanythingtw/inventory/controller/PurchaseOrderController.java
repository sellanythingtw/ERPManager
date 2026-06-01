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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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

    @GetMapping("/purchases")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String supplierCode,
                       @RequestParam(required = false) String supplierName,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                       Model model) {
        var suppliers = supplierRepository.findAll();
        var orders = purchaseOrderService.listAll().stream().filter(order -> {
            if (hasText(status) && !status.equals(order.getStatus())) return false;
            if (dateFrom != null && order.getPurchaseDate() != null && order.getPurchaseDate().isBefore(dateFrom)) return false;
            if (dateTo != null && order.getPurchaseDate() != null && order.getPurchaseDate().isAfter(dateTo)) return false;

            String supplierText = suppliers.stream()
                    .filter(s -> s.getSupplierId() != null && s.getSupplierId().equals(order.getSupplierId()))
                    .map(s -> safe(s.getSupplierCode()) + " " + safe(s.getSupplierName()))
                    .findFirst().orElse("");

            String supplierCodeText = suppliers.stream()
                    .filter(s -> s.getSupplierId() != null && s.getSupplierId().equals(order.getSupplierId()))
                    .map(s -> safe(s.getSupplierCode()))
                    .findFirst().orElse("");

            String supplierNameText = suppliers.stream()
                    .filter(s -> s.getSupplierId() != null && s.getSupplierId().equals(order.getSupplierId()))
                    .map(s -> safe(s.getSupplierName()))
                    .findFirst().orElse("");

            if (hasText(supplierCode) && !supplierCodeText.contains(supplierCode.trim())) return false;
            if (hasText(supplierName) && !supplierNameText.contains(supplierName.trim())) return false;

            if (hasText(keyword)) {
                String k = keyword.trim().toLowerCase();
                String haystack = (safe(order.getPurchaseNo()) + " " + supplierText + " " + safe(order.getStatus())).toLowerCase();
                if (!haystack.contains(k)) return false;
            }
            return true;
        }).sorted(Comparator.comparing(o -> safe(o.getPurchaseNo()))).toList();
        model.addAttribute("orders", orders);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("supplierCode", supplierCode);
        model.addAttribute("supplierName", supplierName);
        model.addAttribute("status", status);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "purchase/list";
    }

    @GetMapping("/purchases/new")
    public String newPurchase(Model model) {
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("labelTemplates", labelPrintService.listTemplates());
        model.addAttribute("today", LocalDate.now());
        return "purchase/new";
    }

    @GetMapping("/purchases/{purchaseId}")
    public String detail(@PathVariable Long purchaseId,
                         @RequestParam(defaultValue = "false") boolean edit,
                         Model model) {
        Map<String, Object> detail = purchaseOrderService.getDetail(purchaseId);
        model.addAllAttributes(detail);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("labelTemplates", labelPrintService.listTemplates());
        model.addAttribute("editMode", edit);
        return "purchase/detail";
    }

    @PostMapping("/purchases/create-full")
    public String createFullPage(@RequestParam(required = false) Long supplierId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                                 @RequestParam(required = false) Boolean taxEnabled,
                                 @RequestParam(required = false) String note,
                                 @RequestParam(required = false) Long productId,
                                 @RequestParam(defaultValue = "0") BigDecimal wholesalePrice,
                                 @RequestParam(defaultValue = "0") BigDecimal salePrice,
                                 @RequestParam(required = false) String trayQuantityCode,
                                 @RequestParam(required = false) String sizeCode,
                                 @RequestParam(required = false) Integer quantity,
                                 @RequestParam(required = false) Long labelSettingId,
                                 @RequestParam(required = false) String itemNote,
                                 @RequestParam(defaultValue = "draft") String action,
                                 RedirectAttributes redirectAttributes) {
        try {
            boolean confirm = "confirm".equals(action);
            Long id = purchaseOrderService.createWithFirstItem(supplierId, purchaseDate, taxEnabled, note,
                    productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity, labelSettingId, itemNote, confirm).getPurchaseId();
            redirectAttributes.addFlashAttribute("successMessage", confirm ? "進貨單已建立並確認。" : "進貨單草稿已建立。仍可點擊編輯繼續修改。");
            return "redirect:/purchases/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchases/new";
        }
    }

    @PostMapping("/purchases/draft")
    public String createDraftPage(@RequestParam(required = false) Long supplierId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                                  RedirectAttributes redirectAttributes) {
        try {
            Long id = purchaseOrderService.createDraft(supplierId, purchaseDate).getPurchaseId();
            redirectAttributes.addFlashAttribute("successMessage", "進貨單草稿已建立，可先存放於列表，之後再回來編輯或確認。");
            return "redirect:/purchases/" + id + "?edit=true";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchases/new";
        }
    }

    @PostMapping("/purchases/{purchaseId}/save-full")
    public String saveFullPage(@PathVariable Long purchaseId,
                               @RequestParam(required = false) Long supplierId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                               @RequestParam(required = false) Boolean taxEnabled,
                               @RequestParam(required = false) String note,
                               @RequestParam(required = false) List<Long> itemId,
                               @RequestParam(required = false) List<Long> productId,
                               @RequestParam(required = false) List<BigDecimal> wholesalePrice,
                               @RequestParam(required = false) List<BigDecimal> salePrice,
                               @RequestParam(required = false) List<String> trayQuantityCode,
                               @RequestParam(required = false) List<String> sizeCode,
                               @RequestParam(required = false) List<Integer> quantity,
                               @RequestParam(required = false) List<Long> labelSettingId,
                               @RequestParam(required = false) List<String> itemNote,
                               @RequestParam(required = false) Long newProductId,
                               @RequestParam(defaultValue = "0") BigDecimal newWholesalePrice,
                               @RequestParam(defaultValue = "0") BigDecimal newSalePrice,
                               @RequestParam(required = false) String newTrayQuantityCode,
                               @RequestParam(required = false) String newSizeCode,
                               @RequestParam(required = false) Integer newQuantity,
                               @RequestParam(required = false) Long newLabelSettingId,
                               @RequestParam(required = false) String newItemNote,
                               RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.saveFullForm(purchaseId, supplierId, purchaseDate, taxEnabled, note,
                    itemId, productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity, labelSettingId, itemNote,
                    newProductId, newWholesalePrice, newSalePrice, newTrayQuantityCode, newSizeCode, newQuantity, newLabelSettingId, newItemNote);
            redirectAttributes.addFlashAttribute("successMessage", "進貨單已儲存。若為已確認單據，PDF 與進貨貼紙已自動更新。");
            return "redirect:/purchases/" + purchaseId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchases/" + purchaseId + "?edit=true";
        }
    }

    @PostMapping("/purchases/{purchaseId}/header")
    public String updateHeaderPage(@PathVariable Long purchaseId,
                                   @RequestParam(required = false) Long supplierId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                                   @RequestParam(required = false) Boolean taxEnabled,
                                   @RequestParam(required = false) String note,
                                   RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.updateDraftHeader(purchaseId, supplierId, purchaseDate, taxEnabled, note);
            redirectAttributes.addFlashAttribute("successMessage", "進貨單表頭已更新。若為已確認單據，已同步批次資料並重新產生 PDF 與貼紙。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/purchases/{purchaseId}/items")
    public String addItemPage(@PathVariable Long purchaseId,
                              @RequestParam Long productId,
                              @RequestParam(defaultValue = "0") BigDecimal wholesalePrice,
                              @RequestParam(defaultValue = "0") BigDecimal salePrice,
                              @RequestParam(required = false) String trayQuantityCode,
                              @RequestParam(required = false) String sizeCode,
                              @RequestParam Integer quantity,
                              @RequestParam(required = false) Long labelSettingId,
                              @RequestParam(required = false) String itemNote,
                              RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.addItem(purchaseId, productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity, labelSettingId, itemNote);
            redirectAttributes.addFlashAttribute("successMessage", "進貨明細已儲存至草稿。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId + "?edit=true";
    }

    @PostMapping("/purchases/{purchaseId}/items/{itemId}/update")
    public String updateItemPage(@PathVariable Long purchaseId,
                                 @PathVariable Long itemId,
                                 @RequestParam Long productId,
                                 @RequestParam(defaultValue = "0") BigDecimal wholesalePrice,
                                 @RequestParam(defaultValue = "0") BigDecimal salePrice,
                                 @RequestParam(required = false) String trayQuantityCode,
                                 @RequestParam(required = false) String sizeCode,
                                 @RequestParam Integer quantity,
                                 @RequestParam(required = false) Long labelSettingId,
                                 @RequestParam(required = false) String itemNote,
                                 RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.updateItem(purchaseId, itemId, productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity, labelSettingId, itemNote);
            redirectAttributes.addFlashAttribute("successMessage", "進貨明細已更新。若為已確認單據，已同步批次、庫存入庫紀錄並重新產生 PDF 與貼紙。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/purchases/{purchaseId}/items/{itemId}/delete")
    public String deleteItemPage(@PathVariable Long purchaseId, @PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.deleteItem(purchaseId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨明細已刪除。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId + "?edit=true";
    }

    @PostMapping("/purchases/{purchaseId}/confirm")
    public String confirmPage(@PathVariable Long purchaseId, RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.confirm(purchaseId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨單已確認，已建立批次條碼、入庫紀錄、PDF 與貼紙。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/purchases/{purchaseId}/delete-draft")
    public String deleteDraftPage(@PathVariable Long purchaseId, RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.deleteDraft(purchaseId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨草稿已刪除。");
            return "redirect:/purchases";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchases/" + purchaseId;
        }
    }

    @PostMapping("/purchases/{purchaseId}/void")
    public String voidPage(@PathVariable Long purchaseId, RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.voidOrder(purchaseId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨單已作廢，可於單據頁恢復。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/purchases/{purchaseId}/restore")
    public String restorePage(@PathVariable Long purchaseId, RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.restoreOrder(purchaseId);
            redirectAttributes.addFlashAttribute("successMessage", "進貨單已恢復。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchases/" + purchaseId;
    }

    @PostMapping("/labels/lots/{lotId}")
    public String createLabelPage(@PathVariable Long lotId,
                                  @RequestParam Long purchaseId,
                                  RedirectAttributes redirectAttributes) {
        try {
            String path = labelPrintService.createLotLabelPdf(lotId);
            return redirectLocalFile(path);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchases/" + purchaseId;
        }
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
                                       @RequestParam Integer quantity,
                                       @RequestParam(required = false) Long labelSettingId,
                                           @RequestParam(required = false) String itemNote) {
        return ApiResult.ok("進貨明細已加入", "data", purchaseOrderService.addItem(purchaseId, productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity, labelSettingId, itemNote));
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
        try {
            return ApiResult.ok("標籤 PDF 已產生", "path", labelPrintService.createLotLabelPdf(lotId));
        } catch (Exception ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    private String redirectLocalFile(String path) {
        return "redirect:/local-file?path=" + UriUtils.encodeQueryParam(path, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
