package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findBySalesIdOrderByPaymentDateDesc(Long salesId);
}
