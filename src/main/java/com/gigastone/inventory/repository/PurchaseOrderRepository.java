package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPurchaseNo(String purchaseNo);
}
