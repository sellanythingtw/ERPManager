package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.Supplier;
import com.sellanythingtw.inventory.service.MasterDataService;
import com.sellanythingtw.inventory.repository.StockMovementRepository;
import org.springframework.format.annotation.DateTimeFormat;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.time.LocalDate;

@Controller
public class MasterDataController {
    private final MasterDataService masterDataService;
    private final StockMovementRepository stockMovementRepository;

    public MasterDataController(MasterDataService masterDataService,
                                StockMovementRepository stockMovementRepository) {
        this.masterDataService = masterDataService;
        this.stockMovementRepository = stockMovementRepository;
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String productCode,
                           @RequestParam(required = false) String productName,
                           @RequestParam(required = false) String productAlias,
                           @RequestParam(required = false) String color,
                           Model model) {
        var products = masterDataService.listProducts().stream()
                .filter(p -> !hasText(productCode) || safe(p.getProductCode()).contains(productCode.trim()))
                .filter(p -> !hasText(productName) || safe(p.getProductName()).contains(productName.trim()))
                .filter(p -> !hasText(productAlias) || safe(p.getProductAlias()).contains(productAlias.trim()))
                .filter(p -> !hasText(color) || safe(p.getColor()).contains(color.trim()))
                .toList();
        model.addAttribute("products", products);
        model.addAttribute("product", new Product());
        model.addAttribute("productCode", productCode);
        model.addAttribute("productName", productName);
        model.addAttribute("productAlias", productAlias);
        model.addAttribute("color", color);
        return "product/list";
    }

    @PostMapping("/api/products")
    @ResponseBody
    public Map<String, Object> saveProduct(@RequestBody Product product) {
        return ApiResult.ok("產品已儲存", "data", masterDataService.saveProduct(product));
    }

    @GetMapping("/customers")
    public String customers(@RequestParam(required = false) String customerCode,
                            @RequestParam(required = false) String customerName,
                            Model model) {
        var customers = masterDataService.listCustomers().stream()
                .filter(c -> !hasText(customerCode) || safe(c.getCustomerCode()).contains(customerCode.trim()))
                .filter(c -> !hasText(customerName) || safe(c.getCustomerName()).contains(customerName.trim()))
                .toList();
        model.addAttribute("customers", customers);
        model.addAttribute("customer", new Customer());
        model.addAttribute("customerCode", customerCode);
        model.addAttribute("customerName", customerName);
        return "customer/list";
    }

    @PostMapping("/api/customers")
    @ResponseBody
    public Map<String, Object> saveCustomer(@RequestBody Customer customer) {
        return ApiResult.ok("客戶已儲存", "data", masterDataService.saveCustomer(customer));
    }

    @GetMapping("/suppliers")
    public String suppliers(@RequestParam(required = false) String supplierCode,
                            @RequestParam(required = false) String supplierName,
                            Model model) {
        var suppliers = masterDataService.listSuppliers().stream()
                .filter(s -> !hasText(supplierCode) || safe(s.getSupplierCode()).contains(supplierCode.trim()))
                .filter(s -> !hasText(supplierName) || safe(s.getSupplierName()).contains(supplierName.trim()))
                .toList();
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("supplierCode", supplierCode);
        model.addAttribute("supplierName", supplierName);
        return "supplier/list";
    }

    @PostMapping("/api/suppliers")
    @ResponseBody
    public Map<String, Object> saveSupplier(@RequestBody Supplier supplier) {
        return ApiResult.ok("供應商已儲存", "data", masterDataService.saveSupplier(supplier));
    }


    @GetMapping("/api/products/{productId}/stock-movements")
    @ResponseBody
    public Map<String, Object> productStockMovements(@PathVariable Long productId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        List<Map<String, Object>> rows = stockMovementRepository.findProductMovements(productId, dateFrom, dateTo).stream()
                .map(m -> {
                    String typeText = "PURCHASE_IN".equals(m.getMovementType()) ? "進貨" : ("SALES_OUT".equals(m.getMovementType()) ? "銷貨" : safe(m.getMovementType()));
                    String link = "";
                    if ("PURCHASE".equals(m.getSourceType()) && m.getSourceId() != null) link = "/purchases/" + m.getSourceId();
                    if ("SALES".equals(m.getSourceType()) && m.getSourceId() != null) link = "/sales/" + m.getSourceId();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("movementDate", m.getMovementDate() == null ? "" : m.getMovementDate());
                    row.put("movementType", typeText);
                    row.put("sourceType", safe(m.getSourceType()));
                    row.put("sourceNo", safe(m.getSourceNo()));
                    row.put("sourceLink", link);
                    row.put("barcodeValue", safe(m.getBarcodeValue()));
                    row.put("quantityIn", m.getQuantityIn() == null ? 0 : m.getQuantityIn());
                    row.put("quantityOut", m.getQuantityOut() == null ? 0 : m.getQuantityOut());
                    row.put("unitPrice", m.getUnitPrice() == null ? "" : m.getUnitPrice());
                    row.put("note", safe(m.getNote()));
                    return row;
                }).toList();
        return ApiResult.ok("OK", "data", rows);
    }


    @PostMapping("/api/products/{productId}/void")
    @ResponseBody
    public Map<String, Object> voidProduct(@PathVariable Long productId) {
        return ApiResult.ok("產品已作廢", "data", masterDataService.voidProduct(productId));
    }

    @PostMapping("/api/products/{productId}/restore")
    @ResponseBody
    public Map<String, Object> restoreProduct(@PathVariable Long productId) {
        return ApiResult.ok("產品已恢復", "data", masterDataService.restoreProduct(productId));
    }

    @PostMapping("/api/customers/{customerId}/void")
    @ResponseBody
    public Map<String, Object> voidCustomer(@PathVariable Long customerId) {
        return ApiResult.ok("客戶已作廢", "data", masterDataService.voidCustomer(customerId));
    }

    @PostMapping("/api/customers/{customerId}/restore")
    @ResponseBody
    public Map<String, Object> restoreCustomer(@PathVariable Long customerId) {
        return ApiResult.ok("客戶已恢復", "data", masterDataService.restoreCustomer(customerId));
    }

    @PostMapping("/api/suppliers/{supplierId}/void")
    @ResponseBody
    public Map<String, Object> voidSupplier(@PathVariable Long supplierId) {
        return ApiResult.ok("供應商已作廢", "data", masterDataService.voidSupplier(supplierId));
    }

    @PostMapping("/api/suppliers/{supplierId}/restore")
    @ResponseBody
    public Map<String, Object> restoreSupplier(@PathVariable Long supplierId) {
        return ApiResult.ok("供應商已恢復", "data", masterDataService.restoreSupplier(supplierId));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
