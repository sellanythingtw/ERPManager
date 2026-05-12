package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.Supplier;
import com.sellanythingtw.inventory.service.MasterDataService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class MasterDataController {
    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", masterDataService.listProducts());
        model.addAttribute("product", new Product());
        return "product/list";
    }

    @PostMapping("/api/products")
    @ResponseBody
    public Map<String, Object> saveProduct(@RequestBody Product product) {
        return ApiResult.ok("產品已儲存", "data", masterDataService.saveProduct(product));
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", masterDataService.listCustomers());
        model.addAttribute("customer", new Customer());
        return "customer/list";
    }

    @PostMapping("/api/customers")
    @ResponseBody
    public Map<String, Object> saveCustomer(@RequestBody Customer customer) {
        return ApiResult.ok("客戶已儲存", "data", masterDataService.saveCustomer(customer));
    }

    @GetMapping("/suppliers")
    public String suppliers(Model model) {
        model.addAttribute("suppliers", masterDataService.listSuppliers());
        model.addAttribute("supplier", new Supplier());
        return "supplier/list";
    }

    @PostMapping("/api/suppliers")
    @ResponseBody
    public Map<String, Object> saveSupplier(@RequestBody Supplier supplier) {
        return ApiResult.ok("供應商已儲存", "data", masterDataService.saveSupplier(supplier));
    }
}
