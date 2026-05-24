package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.PaymentRecord;
import com.sellanythingtw.inventory.entity.SalesOrder;
import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.repository.PaymentRecordRepository;
import com.sellanythingtw.inventory.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private final SalesOrderRepository salesOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final CustomerRepository customerRepository;

    public PaymentService(SalesOrderRepository salesOrderRepository,
                          PaymentRecordRepository paymentRecordRepository,
                          CustomerRepository customerRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.customerRepository = customerRepository;
    }

    public List<SalesOrder> listReceivables() {
        return salesOrderRepository.findByPaymentStatusInOrderBySalesDateDesc(List.of("UNPAID", "PARTIAL"));
    }

    public List<Map<String, Object>> searchReceivables(String customerCode,
                                                       String customerName,
                                                       LocalDate dateFrom,
                                                       LocalDate dateTo,
                                                       String paymentStatus,
                                                       String paymentType) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalesOrder order : salesOrderRepository.findAll()) {
            if (!"CONFIRMED".equals(order.getStatus())) continue;
            if (dateFrom != null && order.getSalesDate() != null && order.getSalesDate().isBefore(dateFrom)) continue;
            if (dateTo != null && order.getSalesDate() != null && order.getSalesDate().isAfter(dateTo)) continue;
            if (hasText(paymentStatus)) {
                BigDecimal unpaid = order.getUnpaidAmount() == null ? BigDecimal.ZERO : order.getUnpaidAmount();
                if ("PAID".equals(paymentStatus) && unpaid.compareTo(BigDecimal.ZERO) > 0) continue;
                if (("DEBT".equals(paymentStatus) || "UNPAID".equals(paymentStatus)) && unpaid.compareTo(BigDecimal.ZERO) <= 0) continue;
            }
            if (hasText(paymentType) && !paymentType.equals(order.getPaymentType())) continue;

            Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
            if (hasText(customerCode) && (customer == null || customer.getCustomerCode() == null || !customer.getCustomerCode().contains(customerCode))) continue;
            if (hasText(customerName) && (customer == null || customer.getCustomerName() == null || !customer.getCustomerName().contains(customerName))) continue;

            List<PaymentRecord> records = paymentRecordRepository.findBySalesIdOrderByPaymentDateDesc(order.getSalesId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("customer", customer);
            row.put("lastPaymentDate", records.isEmpty() ? null : records.get(0).getPaymentDate());
            rows.add(row);
        }
        rows.sort((a, b) -> {
            SalesOrder oa = (SalesOrder) a.get("order");
            SalesOrder ob = (SalesOrder) b.get("order");
            LocalDate da = oa.getSalesDate() == null ? LocalDate.MIN : oa.getSalesDate();
            LocalDate db = ob.getSalesDate() == null ? LocalDate.MIN : ob.getSalesDate();
            return db.compareTo(da);
        });
        return rows;
    }

    @Transactional
    public PaymentRecord receivePayment(Long salesId, String method, BigDecimal amount, LocalDate paymentDate, String note) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("收款金額必須大於 0");
        if (order.getUnpaidAmount() == null || order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("此銷貨單沒有未收款");
        if (amount.compareTo(order.getUnpaidAmount()) > 0) throw new IllegalArgumentException("收款金額不可大於未收金額");

        PaymentRecord record = new PaymentRecord();
        record.setSalesId(order.getSalesId());
        record.setSalesNo(order.getSalesNo());
        record.setCustomerId(order.getCustomerId());
        record.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
        record.setPaymentMethod(hasText(method) ? method : "CASH");
        record.setAmount(amount);
        record.setNote(note);
        paymentRecordRepository.save(record);

        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        order.setPaidAmount(paid.add(amount));
        order.setUnpaidAmount(order.getTotalAmount().subtract(order.getPaidAmount()));
        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setUnpaidAmount(BigDecimal.ZERO);
            order.setPaymentStatus("PAID");
            order.setPaymentType(record.getPaymentMethod());
        } else {
            order.setPaymentStatus("PARTIAL");
        }
        salesOrderRepository.save(order);
        return record;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
