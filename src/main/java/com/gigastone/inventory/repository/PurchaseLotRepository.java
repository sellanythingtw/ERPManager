package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.PurchaseLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseLotRepository extends JpaRepository<PurchaseLot, Long> {
    Optional<PurchaseLot> findByBarcodeValue(String barcodeValue);
    List<PurchaseLot> findByPurchaseId(Long purchaseId);
    List<PurchaseLot> findByProductIdAndStatus(Long productId, String status);
}
