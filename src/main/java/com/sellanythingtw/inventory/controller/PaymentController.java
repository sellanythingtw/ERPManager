package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.service.PaymentService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments/receivables")
    public String receivables(@RequestParam(required = false) String customerCode,
                              @RequestParam(required = false) String customerName,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                              @RequestParam(required = false, defaultValue = "UNPAID") String paymentStatus,
                              @RequestParam(required = false) String paymentType,
                              Model model) {
        model.addAttribute("rows", paymentService.searchReceivables(customerCode, customerName, dateFrom, dateTo, paymentStatus, paymentType));
        model.addAttribute("customerCode", customerCode);
        model.addAttribute("customerName", customerName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("paymentType", paymentType);
        model.addAttribute("today", LocalDate.now());
        return "payment/receivables";
    }

    @PostMapping("/payments/{salesId}/receive")
    public String receivePage(@PathVariable Long salesId,
                              @RequestParam String method,
                              @RequestParam BigDecimal amount,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                              @RequestParam(required = false) String note) {
        paymentService.receivePayment(salesId, method, amount, paymentDate, note);
        return "redirect:/payments/receivables?received=1";
    }

    @PostMapping("/api/payments/{salesId}/receive")
    @ResponseBody
    public Map<String, Object> receive(@PathVariable Long salesId,
                                       @RequestParam String method,
                                       @RequestParam BigDecimal amount,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                                       @RequestParam(required = false) String note) {
        return ApiResult.ok("沖帳完成", "data", paymentService.receivePayment(salesId, method, amount, paymentDate, note));
    }
}
