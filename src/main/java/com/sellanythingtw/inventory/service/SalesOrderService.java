package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.*;
import com.sellanythingtw.inventory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesOrderService {
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository itemRepository;
    private final PurchaseLotRepository lotRepository;
    private final CustomerRepository customerRepository;
    private final StockMovementRepository movementRepository;
    private final NumberSequenceService sequenceService;
    private final PdfService pdfService;
    private final CloudSyncService cloudSyncService;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository itemRepository,
                             PurchaseLotRepository lotRepository,
                             CustomerRepository customerRepository,
                             StockMovementRepository movementRepository,
                             NumberSequenceService sequenceService,
                             PdfService pdfService,
                             CloudSyncService cloudSyncService) {
        this.salesOrderRepository = salesOrderRepository;
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
        this.sequenceService = sequenceService;
        this.pdfService = pdfService;
        this.cloudSyncService = cloudSyncService;
    }

    public SalesOrder createDraft(Long customerId, LocalDate salesDate) {
        LocalDate today = LocalDate.now();
        SalesOrder order = new SalesOrder();
        order.setSalesNo(sequenceService.nextDocumentNo("SO", today.format(YMD)));
        order.setDocumentDate(today);
        order.setSalesDate(salesDate == null ? today : salesDate);
        order.setCustomerId(customerId);
        return salesOrderRepository.save(order);
    }


    public SalesOrder getOrder(Long salesId) {
        return salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
    }

    public List<SalesOrderItem> listItems(Long salesId) {
        return itemRepository.findBySalesIdOrderBySortOrderAsc(salesId);
    }

    public Map<String, Object> getDetail(Long salesId) {
        Map<String, Object> result = new LinkedHashMap<>();
        SalesOrder order = getOrder(salesId);
        result.put("order", order);
        result.put("items", listItems(salesId));
        customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId())
                .ifPresent(customer -> result.put("customer", customer));
        return result;
    }

    public Map<String, Object> lookupBarcode(String barcodeValue) {
        PurchaseLot lot = lotRepository.findByBarcodeValue(barcodeValue)
                .orElseThrow(() -> new IllegalArgumentException("找不到條碼資料"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lotId", lot.getLotId());
        result.put("barcodeValue", lot.getBarcodeValue());
        result.put("productCode", lot.getProductCode());
        result.put("productName", lot.getProductName());
        result.put("productAlias", lot.getProductAlias());
        result.put("supplierCode", lot.getSupplierCode());
        result.put("purchaseDate", lot.getPurchaseDate());
        result.put("salePrice", lot.getSalePrice());
        result.put("remainingQuantity", lot.getRemainingQuantity());
        result.put("status", lot.getStatus());
        return result;
    }

    @Transactional
    public SalesOrderItem addItemByBarcode(Long salesId, String barcodeValue, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("銷貨數量必須大於 0");
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿銷貨單可以新增明細");
        PurchaseLot lot = lotRepository.findByBarcodeValue(barcodeValue)
                .orElseThrow(() -> new IllegalArgumentException("找不到條碼資料"));
        if (!"ACTIVE".equals(lot.getStatus())) throw new IllegalStateException("此批次已不可銷貨");
        if (quantity > lot.getRemainingQuantity()) {
            throw new IllegalStateException("此批次庫存不足，目前剩餘：" + lot.getRemainingQuantity());
        }

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesId(salesId);
        item.setLotId(lot.getLotId());
        item.setBarcodeValue(lot.getBarcodeValue());
        item.setProductId(lot.getProductId());
        item.setProductCode(lot.getProductCode());
        item.setProductName(lot.getProductName());
        item.setProductAlias(lot.getProductAlias());
        item.setPurchaseDate(lot.getPurchaseDate());
        item.setSupplierCode(lot.getSupplierCode());
        item.setUnitPrice(lot.getSalePrice());
        item.setQuantity(quantity);
        item.setAmount(lot.getSalePrice().multiply(BigDecimal.valueOf(quantity)));
        item.setSortOrder(itemRepository.findBySalesIdOrderBySortOrderAsc(salesId).size() + 1);
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long salesId, Long itemId) {
        SalesOrder order = getOrder(salesId);
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿銷貨單可以刪除明細");
        SalesOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨明細"));
        if (!salesId.equals(item.getSalesId())) throw new IllegalArgumentException("明細不屬於此銷貨單");
        itemRepository.delete(item);
    }

    @Transactional
    public SalesOrder confirm(Long salesId, String paymentType) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿銷貨單可以確認");
        List<SalesOrderItem> items = itemRepository.findBySalesIdOrderBySortOrderAsc(salesId);
        if (items.isEmpty()) throw new IllegalStateException("銷貨單沒有明細");

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (SalesOrderItem item : items) {
            PurchaseLot lot = lotRepository.findById(item.getLotId())
                    .orElseThrow(() -> new IllegalStateException("找不到進貨批次"));
            if (item.getQuantity() > lot.getRemainingQuantity()) {
                throw new IllegalStateException("批次 " + lot.getBarcodeValue() + " 庫存不足，目前剩餘：" + lot.getRemainingQuantity());
            }
            lot.setRemainingQuantity(lot.getRemainingQuantity() - item.getQuantity());
            if (lot.getRemainingQuantity() <= 0) lot.setStatus("EMPTY");
            lotRepository.save(lot);

            subtotal = subtotal.add(item.getAmount());
            totalQuantity += item.getQuantity();

            StockMovement movement = new StockMovement();
            movement.setProductId(item.getProductId());
            movement.setLotId(item.getLotId());
            movement.setBarcodeValue(item.getBarcodeValue());
            movement.setMovementType("SALES_OUT");
            movement.setSourceType("SALES");
            movement.setSourceId(order.getSalesId());
            movement.setSourceNo(order.getSalesNo());
            movement.setMovementDate(order.getSalesDate());
            movement.setQuantityIn(0);
            movement.setQuantityOut(item.getQuantity());
            movement.setUnitPrice(item.getUnitPrice());
            movementRepository.save(movement);
        }

        BigDecimal tax = Boolean.TRUE.equals(order.getTaxEnabled())
                ? subtotal.multiply(order.getTaxRate()).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(tax));
        order.setTotalQuantity(totalQuantity);
        order.setPaymentType(paymentType == null || paymentType.isBlank() ? "CASH" : paymentType);
        if ("CREDIT".equals(order.getPaymentType())) {
            order.setPaidAmount(BigDecimal.ZERO);
            order.setUnpaidAmount(order.getTotalAmount());
            order.setPaymentStatus("UNPAID");
        } else {
            order.setPaidAmount(order.getTotalAmount());
            order.setUnpaidAmount(BigDecimal.ZERO);
            order.setPaymentStatus("PAID");
        }
        order.setStatus("CONFIRMED");
        order = salesOrderRepository.save(order);

        Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
        String pdfPath = pdfService.createSalesOrderPdf(order, items, customer);
        order.setPdfPath(pdfPath);
        order = salesOrderRepository.save(order);
        cloudSyncService.uploadPdfIfEnabled("SALES", order.getSalesId(), pdfPath, customer == null ? "未指定客戶" : customer.getCustomerName());
        return order;
    }
}
