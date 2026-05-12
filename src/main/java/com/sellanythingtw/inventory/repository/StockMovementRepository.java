package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("select coalesce(sum(m.quantityIn),0) - coalesce(sum(m.quantityOut),0) from StockMovement m where m.productId = :productId")
    Integer getProductQuantity(@Param("productId") Long productId);
}
