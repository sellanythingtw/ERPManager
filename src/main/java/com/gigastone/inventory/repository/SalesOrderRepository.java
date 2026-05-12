package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    List<SalesOrder> findByPaymentStatusInOrderBySalesDateDesc(List<String> statuses);
}
