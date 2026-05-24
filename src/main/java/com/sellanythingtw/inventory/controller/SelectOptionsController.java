package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.LabelPrintSetting;
import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.Supplier;
import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.SupplierRepository;
import com.sellanythingtw.inventory.service.LabelPrintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SelectOptionsController {
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final LabelPrintService labelPrintService;

    public SelectOptionsController(CustomerRepository customerRepository,
                                   SupplierRepository supplierRepository,
                                   ProductRepository productRepository,
                                   LabelPrintService labelPrintService) {
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.labelPrintService = labelPrintService;
    }


    @GetMapping("/api/lookup/customers")
    public List<Map<String, Object>> lookupCustomers(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Long id) {
        return customerRepository.findAll().stream()
                .filter(c -> id == null || id.equals(c.getCustomerId()))
                .filter(c -> id != null || c.getActive() == null || Boolean.TRUE.equals(c.getActive()))
                .filter(c -> matches(q, c.getCustomerCode(), c.getCustomerName(), c.getPhone()))
                .map(this::customerOption)
                .toList();
    }

    @GetMapping("/api/lookup/suppliers")
    public List<Map<String, Object>> lookupSuppliers(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Long id) {
        return supplierRepository.findAll().stream()
                .filter(s -> id == null || id.equals(s.getSupplierId()))
                .filter(s -> id != null || s.getActive() == null || Boolean.TRUE.equals(s.getActive()))
                .filter(s -> matches(q, s.getSupplierCode(), s.getSupplierName(), s.getPhone(), s.getContactPerson()))
                .map(this::supplierOption)
                .toList();
    }

    @GetMapping("/api/lookup/products")
    public List<Map<String, Object>> lookupProducts(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long id) {
        return productRepository.findAll().stream()
                .filter(p -> id == null || id.equals(p.getProductId()))
                .filter(p -> id != null || p.getActive() == null || Boolean.TRUE.equals(p.getActive()))
                .filter(p -> matches(q, p.getProductCode(), p.getProductName(), p.getProductAlias(), p.getCategory(), p.getColor()))
                .map(this::productOption)
                .toList();
    }

    @GetMapping("/api/lookup/label-templates")
    public List<Map<String, Object>> lookupLabelTemplates(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) Long id) {
        return labelPrintService.listTemplates().stream()
                .filter(t -> id == null || id.equals(t.getSettingId()))
                .filter(t -> matches(q, t.getTemplateName(), Boolean.TRUE.equals(t.getDefaultTemplate()) ? "預設" : ""))
                .map(this::labelOption)
                .toList();
    }

    @GetMapping("/api/options/suppliers")
    public List<Map<String, Object>> suppliers() {
        return supplierRepository.findAll().stream().filter(s -> s.getActive() == null || Boolean.TRUE.equals(s.getActive())).map(this::supplierOption).toList();
    }

    @GetMapping("/api/options/products")
    public List<Map<String, Object>> products() {
        return productRepository.findAll().stream().filter(p -> p.getActive() == null || Boolean.TRUE.equals(p.getActive())).map(this::productOption).toList();
    }

    @GetMapping("/api/options/label-templates")
    public List<Map<String, Object>> labelTemplates() {
        return labelPrintService.listTemplates().stream().map(this::labelOption).toList();
    }

    private Map<String, Object> customerOption(Customer c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getCustomerId());
        map.put("code", nvl(c.getCustomerCode()));
        map.put("name", nvl(c.getCustomerName()));
        map.put("label", nvl(c.getCustomerCode()) + "｜" + nvl(c.getCustomerName()));
        return map;
    }

    private Map<String, Object> supplierOption(Supplier s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getSupplierId());
        map.put("code", nvl(s.getSupplierCode()));
        map.put("name", nvl(s.getSupplierName()));
        map.put("label", nvl(s.getSupplierCode()) + "｜" + nvl(s.getSupplierName()));
        return map;
    }

    private Map<String, Object> productOption(Product p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getProductId());
        map.put("code", nvl(p.getProductCode()));
        map.put("name", nvl(p.getProductName()));
        map.put("label", nvl(p.getProductCode()) + "｜" + nvl(p.getProductName()) + "｜" + nvl(p.getProductAlias()));
        return map;
    }

    private Map<String, Object> labelOption(LabelPrintSetting t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getSettingId());
        map.put("code", String.valueOf(t.getSettingId()));
        map.put("name", nvl(t.getTemplateName()));
        map.put("label", nvl(t.getTemplateName()) + (Boolean.TRUE.equals(t.getDefaultTemplate()) ? "（預設）" : ""));
        return map;
    }

    private boolean matches(String q, Object... fields) {
        if (q == null || q.trim().isEmpty()) return true;
        String key = q.trim().toLowerCase();
        for (Object field : fields) {
            if (field != null && String.valueOf(field).toLowerCase().contains(key)) return true;
        }
        return false;
    }

    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
}
