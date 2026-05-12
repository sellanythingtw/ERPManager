package com.gigastone.inventory.service;

import com.gigastone.inventory.entity.PaymentRecord;
import com.gigastone.inventory.entity.SalesOrder;
import com.gigastone.inventory.repository.PaymentRecordRepository;
import com.gigastone.inventory.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {
    private final SalesOrderRepository salesOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentService(SalesOrderRepository salesOrderRepository, PaymentRecordRepository paymentRecordRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public List<SalesOrder> listReceivables() {
        return salesOrderRepository.findByPaymentStatusInOrderBySalesDateDesc(List.of("UNPAID", "PARTIAL"));
    }

    @Transactional
    public PaymentRecord receivePayment(Long salesId, String method, BigDecimal amount, LocalDate paymentDate, String note) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("收款金額必須大於 0");
        if (amount.compareTo(order.getUnpaidAmount()) > 0) throw new IllegalArgumentException("收款金額不可大於未收金額");

        PaymentRecord record = new PaymentRecord();
        record.setSalesId(order.getSalesId());
        record.setSalesNo(order.getSalesNo());
        record.setCustomerId(order.getCustomerId());
        record.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
        record.setPaymentMethod(method == null || method.isBlank() ? "CASH" : method);
        record.setAmount(amount);
        record.setNote(note);
        paymentRecordRepository.save(record);

        order.setPaidAmount(order.getPaidAmount().add(amount));
        order.setUnpaidAmount(order.getTotalAmount().subtract(order.getPaidAmount()));
        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setUnpaidAmount(BigDecimal.ZERO);
            order.setPaymentStatus("PAID");
        } else {
            order.setPaymentStatus("PARTIAL");
        }
        salesOrderRepository.save(order);
        return record;
    }
}
