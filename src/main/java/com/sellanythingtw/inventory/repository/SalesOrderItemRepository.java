package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {
    List<SalesOrderItem> findBySalesIdOrderBySortOrderAsc(Long salesId);
}
