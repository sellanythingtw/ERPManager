package com.sellanythingtw.inventory.controller;

import com.sellanythingtw.inventory.service.PaymentService;
import com.sellanythingtw.inventory.utils.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
                              @RequestParam(required = false) String paymentStatus,
                              @RequestParam(required = false) String paymentType,
                              Model model) {
        var rows = paymentService.searchReceivables(customerCode, customerName, dateFrom, dateTo, paymentStatus, paymentType);
        model.addAttribute("rows", rows);
        model.addAttribute("summary", paymentService.receivablesSummary(rows));
        model.addAttribute("customerCode", customerCode);
        model.addAttribute("customerName", customerName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("paymentType", paymentType);
        model.addAttribute("today", LocalDate.now());
        return "payment/receivables";
    }


    @GetMapping("/payments/receivables/{salesId}")
    public String receiveDetail(@PathVariable Long salesId, Model model) {
        Map<String, Object> detail = paymentService.getPaymentDetail(salesId);
        model.addAllAttributes(detail);
        model.addAttribute("today", LocalDate.now());
        return "payment/receive";
    }



    @PostMapping("/payments/receivables/{salesId}/records/{paymentId}/delete")
    public String deletePaymentRecord(@PathVariable Long salesId,
                                      @PathVariable Long paymentId) {
        paymentService.deletePaymentRecord(salesId, paymentId);
        return "redirect:/payments/receivables/" + salesId + "?deleted=1";
    }

    @PostMapping("/payments/receivables/{salesId}/records/{paymentId}/update")
    public String updatePaymentRecord(@PathVariable Long salesId,
                                      @PathVariable Long paymentId,
                                      @RequestParam String method,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                                      @RequestParam(required = false) String note,
                                      @RequestParam(required = false) List<Long> paymentItemIds,
                                      @RequestParam(required = false) List<Integer> receivedQuantities,
                                      @RequestParam(required = false) List<BigDecimal> receivedAmounts) {
        paymentService.updatePaymentRecordItems(salesId, paymentId, method, paymentDate, note, paymentItemIds, receivedQuantities, receivedAmounts);
        return "redirect:/payments/receivables/" + salesId + "?updated=1";
    }

    @PostMapping("/payments/receivables/{salesId}/receive-items")
    public String receiveItems(@PathVariable Long salesId,
                               @RequestParam String method,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                               @RequestParam(required = false) String note,
                               @RequestParam(required = false) List<Long> salesItemIds,
                               @RequestParam(required = false) List<Integer> receivedQuantities,
                               @RequestParam(required = false) List<BigDecimal> receivedAmounts) {
        paymentService.receivePaymentByItems(salesId, method, paymentDate, note, salesItemIds, receivedQuantities, receivedAmounts);
        return "redirect:/payments/receivables/" + salesId + "?received=1";
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
