package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPurchaseNo(String purchaseNo);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
}
