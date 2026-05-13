package com.sellanythingtw.inventory.repository;

import com.sellanythingtw.inventory.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    List<SalesOrder> findByPaymentStatusInOrderBySalesDateDesc(List<String> statuses);
    List<SalesOrder> findAllByOrderByCreatedAtDesc();
}
