package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.SalesOrder;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.PurchaseOrderRepository;
import com.sellanythingtw.inventory.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public DashboardService(PurchaseOrderRepository purchaseOrderRepository,
                            SalesOrderRepository salesOrderRepository,
                            ProductRepository productRepository,
                            InventoryService inventoryService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    public Map<String, Object> summary() {
        LocalDate today = LocalDate.now();
        long todayPurchases = purchaseOrderRepository.findAll().stream()
                .filter(o -> today.equals(o.getPurchaseDate()))
                .count();
        long todaySales = salesOrderRepository.findAll().stream()
                .filter(o -> today.equals(o.getSalesDate()))
                .count();
        BigDecimal todayReceived = salesOrderRepository.findAll().stream()
                .filter(o -> today.equals(o.getSalesDate()))
                .map(o -> o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceivable = salesOrderRepository.findAll().stream()
                .filter(o -> "CONFIRMED".equals(o.getStatus()))
                .map(o -> o.getUnpaidAmount() == null ? BigDecimal.ZERO : o.getUnpaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long draftSales = salesOrderRepository.findAll().stream().filter(o -> "DRAFT".equals(o.getStatus())).count();
        long unpaidSales = salesOrderRepository.findAll().stream()
                .filter(o -> "CONFIRMED".equals(o.getStatus()))
                .filter(o -> o.getUnpaidAmount() != null && o.getUnpaidAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();
        long lowStock = inventoryService.searchRealtimeInventory(null, null).stream()
                .filter(r -> Boolean.TRUE.equals(r.get("lowStock")))
                .count();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("todayPurchases", todayPurchases);
        map.put("todaySales", todaySales);
        map.put("todayReceived", todayReceived);
        map.put("totalReceivable", totalReceivable);
        map.put("draftSales", draftSales);
        map.put("unpaidSales", unpaidSales);
        map.put("lowStock", lowStock);
        map.put("productCount", productRepository.count());
        return map;
    }
}
