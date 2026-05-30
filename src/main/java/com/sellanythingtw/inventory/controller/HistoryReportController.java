package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.service.HistoryReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class HistoryReportController {
    private final HistoryReportService historyReportService;

    public HistoryReportController(HistoryReportService historyReportService) {
        this.historyReportService = historyReportService;
    }

    @GetMapping("/reports/supplier-purchases")
    public String supplierPurchases(@RequestParam(required = false) String supplierCode,
                                    @RequestParam(required = false) String supplierName,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                    Model model) {
        List<Map<String, Object>> rows = historyReportService.supplierPurchases(supplierCode, supplierName, dateFrom, dateTo);
        model.addAttribute("rows", rows);
        model.addAttribute("summary", historyReportService.simpleSummary(rows));
        model.addAttribute("supplierCode", supplierCode);
        model.addAttribute("supplierName", supplierName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "reports/supplier-purchases";
    }

    @GetMapping("/reports/customer-sales")
    public String customerSales(@RequestParam(required = false) String customerCode,
                                @RequestParam(required = false) String customerName,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                Model model) {
        List<Map<String, Object>> rows = historyReportService.customerSales(customerCode, customerName, dateFrom, dateTo);
        model.addAttribute("rows", rows);
        model.addAttribute("summary", historyReportService.simpleSummary(rows));
        model.addAttribute("customerCode", customerCode);
        model.addAttribute("customerName", customerName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "reports/customer-sales";
    }

    @GetMapping("/reports/stock-movements")
    public String stockMovements(@RequestParam(required = false) String productCode,
                                 @RequestParam(required = false) String productName,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                 Model model) {
        List<Map<String, Object>> rows = historyReportService.stockMovements(productCode, productName, dateFrom, dateTo);
        model.addAttribute("rows", rows);
        model.addAttribute("summary", historyReportService.simpleSummary(rows));
        model.addAttribute("productCode", productCode);
        model.addAttribute("productName", productName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "reports/stock-movements";
    }
}
