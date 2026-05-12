package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByPurchaseIdOrderBySortOrderAsc(Long purchaseId);
}
