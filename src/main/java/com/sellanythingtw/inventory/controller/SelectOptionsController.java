package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.Supplier;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.SupplierRepository;
import com.sellanythingtw.inventory.service.LabelPrintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SelectOptionsController {
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final LabelPrintService labelPrintService;

    public SelectOptionsController(SupplierRepository supplierRepository,
                                   ProductRepository productRepository,
                                   LabelPrintService labelPrintService) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.labelPrintService = labelPrintService;
    }

    @GetMapping("/api/options/suppliers")
    public List<Map<String, Object>> suppliers() {
        return supplierRepository.findAll().stream().map(this::supplierOption).toList();
    }

    @GetMapping("/api/options/products")
    public List<Map<String, Object>> products() {
        return productRepository.findAll().stream().map(this::productOption).toList();
    }

    @GetMapping("/api/options/label-templates")
    public List<Map<String, Object>> labelTemplates() {
        return labelPrintService.listTemplates().stream().map(this::labelOption).toList();
    }

    private Map<String, Object> supplierOption(Supplier s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getSupplierId());
        map.put("label", nvl(s.getSupplierName()));
        return map;
    }

    private Map<String, Object> productOption(Product p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getProductId());
        map.put("label", nvl(p.getProductCode()) + "｜" + nvl(p.getProductName()) + "｜" + nvl(p.getProductAlias()));
        return map;
    }

    private Map<String, Object> labelOption(LabelPrintSetting t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getSettingId());
        map.put("label", nvl(t.getTemplateName()) + (Boolean.TRUE.equals(t.getDefaultTemplate()) ? "（預設）" : ""));
        return map;
    }

    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
}
