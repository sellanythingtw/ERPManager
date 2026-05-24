package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("select coalesce(sum(m.quantityIn),0) - coalesce(sum(m.quantityOut),0) from StockMovement m where m.productId = :productId")
    Integer getProductQuantity(@Param("productId") Long productId);
    Optional<StockMovement> findFirstBySourceTypeAndSourceIdAndLotIdAndMovementType(String sourceType, Long sourceId, Long lotId, String movementType);

    @Query("select m from StockMovement m where m.productId = :productId " +
           "and (:dateFrom is null or m.movementDate >= :dateFrom) " +
           "and (:dateTo is null or m.movementDate <= :dateTo) " +
           "order by m.movementDate desc, m.createdAt desc")
    List<StockMovement> findProductMovements(@Param("productId") Long productId,
                                             @Param("dateFrom") LocalDate dateFrom,
                                             @Param("dateTo") LocalDate dateTo);
}
