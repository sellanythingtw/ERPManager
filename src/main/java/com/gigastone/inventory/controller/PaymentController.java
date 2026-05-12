package com.gigastone.inventory.controller;

import com.gigastone.inventory.service.PaymentService;
import com.gigastone.inventory.utils.ApiResult;
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
    public String receivables(Model model) {
        model.addAttribute("orders", paymentService.listReceivables());
        return "payment/receivables";
    }

    @PostMapping("/api/payments/{salesId}/receive")
    @ResponseBody
    public Map<String, Object> receive(@PathVariable Long salesId,
                                       @RequestParam String method,
                                       @RequestParam BigDecimal amount,
                                       @RequestParam(required = false) LocalDate paymentDate,
                                       @RequestParam(required = false) String note) {
        return ApiResult.ok("沖帳完成", "data", paymentService.receivePayment(salesId, method, amount, paymentDate, note));
    }
}
