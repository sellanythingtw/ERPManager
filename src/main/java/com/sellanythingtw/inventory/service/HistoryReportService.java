package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.*;
import com.sellanythingtw.inventory.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class HistoryReportService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public HistoryReportService(PurchaseOrderRepository purchaseOrderRepository,
                                PurchaseOrderItemRepository purchaseOrderItemRepository,
                                SupplierRepository supplierRepository,
                                SalesOrderRepository salesOrderRepository,
                                SalesOrderItemRepository salesOrderItemRepository,
                                CustomerRepository customerRepository,
                                ProductRepository productRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.supplierRepository = supplierRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public List<Map<String, Object>> supplierPurchases(String supplierCode, String supplierName, LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PurchaseOrder order : purchaseOrderRepository.findAllByOrderByCreatedAtDesc()) {
            if ("DRAFT".equals(order.getStatus())) continue;
            if (dateFrom != null && order.getPurchaseDate() != null && order.getPurchaseDate().isBefore(dateFrom)) continue;
            if (dateTo != null && order.getPurchaseDate() != null && order.getPurchaseDate().isAfter(dateTo)) continue;
            Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
            if (hasText(supplierCode) && (supplier == null || !safe(supplier.getSupplierCode()).contains(supplierCode.trim()))) continue;
            if (hasText(supplierName) && (supplier == null || !safe(supplier.getSupplierName()).contains(supplierName.trim()))) continue;
            for (PurchaseOrderItem item : purchaseOrderItemRepository.findByPurchaseIdOrderBySortOrderAsc(order.getPurchaseId())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("supplierCode", supplier == null ? "" : supplier.getSupplierCode());
                row.put("supplierName", supplier == null ? "未指定" : supplier.getSupplierName());
                row.put("date", order.getPurchaseDate());
                row.put("productCode", item.getProductCode());
                row.put("productName", item.getProductName());
                row.put("productAlias", item.getProductAlias());
                row.put("specification", item.getSpecification());
                row.put("unitPrice", nvl(item.getWholesalePrice()));
                row.put("quantity", nvl(item.getQuantity()));
                row.put("amount", nvl(item.getAmount()));
                row.put("itemNote", item.getItemNote());
                row.put("sourceNo", order.getPurchaseNo());
                rows.add(row);
            }
        }
        return rows;
    }

    public List<Map<String, Object>> customerSales(String customerCode, String customerName, LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalesOrder order : salesOrderRepository.findAllByOrderByCreatedAtDesc()) {
            if (!"CONFIRMED".equals(order.getStatus()) && !"VOID".equals(order.getStatus())) continue;
            if (dateFrom != null && order.getSalesDate() != null && order.getSalesDate().isBefore(dateFrom)) continue;
            if (dateTo != null && order.getSalesDate() != null && order.getSalesDate().isAfter(dateTo)) continue;
            Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
            if (hasText(customerCode) && (customer == null || !safe(customer.getCustomerCode()).contains(customerCode.trim()))) continue;
            if (hasText(customerName) && (customer == null || !safe(customer.getCustomerName()).contains(customerName.trim()))) continue;
            for (SalesOrderItem item : salesOrderItemRepository.findBySalesIdOrderBySortOrderAsc(order.getSalesId())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("customerCode", customer == null ? "" : customer.getCustomerCode());
                row.put("customerName", customer == null ? "未指定" : customer.getCustomerName());
                row.put("date", order.getSalesDate());
                row.put("productCode", item.getProductCode());
                row.put("productName", item.getProductName());
                row.put("productAlias", item.getProductAlias());
                row.put("specification", item.getSpecification());
                row.put("unitPrice", nvl(item.getUnitPrice()));
                row.put("quantity", nvl(item.getQuantity()));
                row.put("amount", nvl(item.getAmount()));
                row.put("itemNote", item.getItemNote());
                row.put("sourceNo", order.getSalesNo());
                rows.add(row);
            }
        }
        return rows;
    }

    public List<Map<String, Object>> stockMovements(String productCode, String productName, LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(purchaseMovements(productCode, productName, dateFrom, dateTo));
        rows.addAll(salesMovements(productCode, productName, dateFrom, dateTo));
        rows.sort((a, b) -> {
            LocalDate da = (LocalDate) a.get("date");
            LocalDate db = (LocalDate) b.get("date");
            da = da == null ? LocalDate.MIN : da;
            db = db == null ? LocalDate.MIN : db;
            return db.compareTo(da);
        });
        return rows;
    }

    private List<Map<String, Object>> purchaseMovements(String productCode, String productName, LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PurchaseOrder order : purchaseOrderRepository.findAllByOrderByCreatedAtDesc()) {
            if ("DRAFT".equals(order.getStatus())) continue;
            if (dateFrom != null && order.getPurchaseDate() != null && order.getPurchaseDate().isBefore(dateFrom)) continue;
            if (dateTo != null && order.getPurchaseDate() != null && order.getPurchaseDate().isAfter(dateTo)) continue;
            Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
            for (PurchaseOrderItem item : purchaseOrderItemRepository.findByPurchaseIdOrderBySortOrderAsc(order.getPurchaseId())) {
                if (!productMatches(item.getProductCode(), item.getProductName(), productCode, productName)) continue;
                Map<String, Object> row = commonProductRow(item.getProductCode(), item.getProductName(), item.getProductAlias(), item.getSpecification(), item.getWholesalePrice(), item.getQuantity(), item.getAmount(), item.getItemNote(), order.getPurchaseDate(), order.getPurchaseNo());
                row.put("type", "進貨");
                row.put("partnerCode", supplier == null ? "" : supplier.getSupplierCode());
                row.put("partnerName", supplier == null ? "未指定" : supplier.getSupplierName());
                row.put("quantityIn", nvl(item.getQuantity()));
                row.put("quantityOut", 0);
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> salesMovements(String productCode, String productName, LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalesOrder order : salesOrderRepository.findAllByOrderByCreatedAtDesc()) {
            if (!"CONFIRMED".equals(order.getStatus()) && !"VOID".equals(order.getStatus())) continue;
            if (dateFrom != null && order.getSalesDate() != null && order.getSalesDate().isBefore(dateFrom)) continue;
            if (dateTo != null && order.getSalesDate() != null && order.getSalesDate().isAfter(dateTo)) continue;
            Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
            for (SalesOrderItem item : salesOrderItemRepository.findBySalesIdOrderBySortOrderAsc(order.getSalesId())) {
                if (!productMatches(item.getProductCode(), item.getProductName(), productCode, productName)) continue;
                Map<String, Object> row = commonProductRow(item.getProductCode(), item.getProductName(), item.getProductAlias(), item.getSpecification(), item.getUnitPrice(), item.getQuantity(), item.getAmount(), item.getItemNote(), order.getSalesDate(), order.getSalesNo());
                row.put("type", "銷貨");
                row.put("partnerCode", customer == null ? "" : customer.getCustomerCode());
                row.put("partnerName", customer == null ? "未指定" : customer.getCustomerName());
                row.put("quantityIn", 0);
                row.put("quantityOut", nvl(item.getQuantity()));
                rows.add(row);
            }
        }
        return rows;
    }

    private Map<String, Object> commonProductRow(String code, String name, String alias, String spec, BigDecimal price, Integer qty, BigDecimal amount, String note, LocalDate date, String sourceNo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", date);
        row.put("productCode", code);
        row.put("productName", name);
        row.put("productAlias", alias);
        row.put("specification", spec);
        row.put("unitPrice", nvl(price));
        row.put("quantity", nvl(qty));
        row.put("amount", nvl(amount));
        row.put("itemNote", note);
        row.put("sourceNo", sourceNo);
        return row;
    }

    private boolean productMatches(String code, String name, String productCode, String productName) {
        if (hasText(productCode) && !safe(code).contains(productCode.trim())) return false;
        return !hasText(productName) || safe(name).contains(productName.trim());
    }

    public Map<String, Object> simpleSummary(List<Map<String, Object>> rows) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;
        for (Map<String, Object> row : rows) {
            Object amount = row.get("amount");
            if (amount instanceof BigDecimal bd) totalAmount = totalAmount.add(bd);
            Object qty = row.get("quantity");
            if (qty instanceof Number n) totalQty += n.intValue();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rowCount", rows.size());
        result.put("totalQuantity", totalQty);
        result.put("totalAmount", totalAmount);
        return result;
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String safe(String value) { return value == null ? "" : value; }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private BigDecimal nvl(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
