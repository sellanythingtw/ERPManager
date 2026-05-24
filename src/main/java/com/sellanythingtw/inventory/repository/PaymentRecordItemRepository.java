package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.PaymentRecordItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRecordItemRepository extends JpaRepository<PaymentRecordItem, Long> {
    List<PaymentRecordItem> findBySalesId(Long salesId);
    List<PaymentRecordItem> findBySalesItemId(Long salesItemId);
    List<PaymentRecordItem> findByPaymentId(Long paymentId);
    List<PaymentRecordItem> findByPaymentIdOrderByPaymentItemIdAsc(Long paymentId);
}
