package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.*;
import com.sellanythingtw.inventory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PaymentRecordRepository paymentRecordRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository itemRepository,
                             PurchaseLotRepository lotRepository,
                             CustomerRepository customerRepository,
                             StockMovementRepository movementRepository,
                             NumberSequenceService sequenceService,
                             PdfService pdfService,
                             CloudSyncService cloudSyncService,
                             PaymentRecordRepository paymentRecordRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
        this.sequenceService = sequenceService;
        this.pdfService = pdfService;
        this.cloudSyncService = cloudSyncService;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public SalesOrder createDraft(Long customerId, LocalDate salesDate) {
        if (customerId == null) throw new IllegalArgumentException("客戶為必填，請雙擊查詢並選擇客戶");
        LocalDate today = LocalDate.now();
        SalesOrder order = new SalesOrder();
        order.setSalesNo(sequenceService.nextDocumentNo("SO", today.format(YMD)));
        order.setDocumentDate(today);
        order.setSalesDate(salesDate == null ? today : salesDate);
        order.setCustomerId(customerId);
        order.setStatus("DRAFT");
        recalculateTotals(order, List.of());
        return salesOrderRepository.save(order);
    }

    @Transactional
    public SalesOrder updateDraftHeader(Long salesId, Long customerId, LocalDate salesDate) {
        return updateHeader(salesId, customerId, salesDate);
    }

    private SalesOrder regenerateSalesPdf(SalesOrder order) {
        order.setPdfUpdatedAt(LocalDateTime.now());
        order = salesOrderRepository.save(order);

        List<SalesOrderItem> items = itemRepository.findBySalesIdOrderBySortOrderAsc(order.getSalesId());
        Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
        String pdfPath = pdfService.createSalesOrderPdf(order, items, customer);
        order.setPdfPath(pdfPath);
        order = salesOrderRepository.save(order);
        cloudSyncService.uploadPdfIfEnabled("SALES", order.getSalesId(), pdfPath, customer == null ? "未指定客戶" : customer.getCustomerName());
        return order;
    }

    @Transactional
    public SalesOrder updateHeader(Long salesId, Long customerId, LocalDate salesDate) {
        if (customerId == null) throw new IllegalArgumentException("客戶為必填，請雙擊查詢並選擇客戶");
        SalesOrder order = getOrder(salesId);
        if ("VOID".equals(order.getStatus())) throw new IllegalStateException("已作廢銷貨單不可修改");
        if ("CONFIRMED".equals(order.getStatus()) && hasPaymentRecords(salesId)) {
            throw new IllegalStateException("此銷貨單已有收款紀錄，請先到沖帳管理刪除或調整收款紀錄後再編輯。");
        }
        order.setCustomerId(customerId);
        if (salesDate != null) order.setSalesDate(salesDate);
        order = salesOrderRepository.save(order);
        if ("CONFIRMED".equals(order.getStatus())) {
            order = regenerateSalesPdf(order);
        }
        return order;
    }

    @Transactional
    public SalesOrder createWithFirstItem(Long customerId,
                                          LocalDate salesDate,
                                          String barcodeValue,
                                          Integer quantity,
                                          String itemNote,
                                          String paymentType,
                                          String paymentStatus,
                                          boolean confirmImmediately) {
        SalesOrder order = createDraft(customerId, salesDate);
        if (barcodeValue != null && !barcodeValue.trim().isEmpty()) {
            addItemByBarcode(order.getSalesId(), barcodeValue.trim(), quantity == null ? 1 : quantity, itemNote);
        }
        if (confirmImmediately) {
            return confirm(order.getSalesId(), paymentType, paymentStatus);
        }
        return getOrder(order.getSalesId());
    }

    public List<SalesOrder> listAll() {
        return salesOrderRepository.findAllByOrderByCreatedAtDesc();
    }


    public List<Map<String, Object>> searchRows(String keyword,
                                                String customerCode,
                                                String customerName,
                                                String status,
                                                LocalDate dateFrom,
                                                LocalDate dateTo) {
        return salesOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
                    row.put("order", order);
                    row.put("customer", customer);
                    return row;
                })
                .filter(row -> {
                    SalesOrder order = (SalesOrder) row.get("order");
                    Customer customer = (Customer) row.get("customer");
                    if (hasText(status) && !status.equals(order.getStatus())) return false;
                    if (dateFrom != null && order.getSalesDate() != null && order.getSalesDate().isBefore(dateFrom)) return false;
                    if (dateTo != null && order.getSalesDate() != null && order.getSalesDate().isAfter(dateTo)) return false;
                    if (hasText(customerCode) && (customer == null || !safe(customer.getCustomerCode()).contains(customerCode.trim()))) return false;
                    if (hasText(customerName) && (customer == null || !safe(customer.getCustomerName()).contains(customerName.trim()))) return false;
                    if (hasText(keyword)) {
                        String k = keyword.trim().toLowerCase();
                        String haystack = (safe(order.getSalesNo()) + " " + safe(order.getStatus()) + " " + safe(order.getPaymentType()) + " "
                                + (customer == null ? "" : safe(customer.getCustomerCode()) + " " + safe(customer.getCustomerName()))).toLowerCase();
                        if (!haystack.contains(k)) return false;
                    }
                    return true;
                })
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }


    public SalesOrder getOrder(Long salesId) {
        return salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
    }


    public boolean hasPaymentRecords(Long salesId) {
        return !paymentRecordRepository.findBySalesIdOrderByPaymentDateDesc(salesId).isEmpty();
    }

    public boolean canReturnToDraft(Long salesId) {
        SalesOrder order = getOrder(salesId);
        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        return "CONFIRMED".equals(order.getStatus())
                && paid.compareTo(BigDecimal.ZERO) <= 0
                && !hasPaymentRecords(salesId);
    }

    public List<SalesOrderItem> listItems(Long salesId) {
        return itemRepository.findBySalesIdOrderBySortOrderAsc(salesId);
    }

    public Map<String, Object> getDetail(Long salesId) {
        Map<String, Object> result = new LinkedHashMap<>();
        SalesOrder order = getOrder(salesId);
        result.put("order", order);
        result.put("hasPaymentRecords", hasPaymentRecords(salesId));
        result.put("canReturnToDraft", canReturnToDraft(salesId));
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
    public SalesOrderItem addItemByBarcode(Long salesId, String barcodeValue, int quantity, String itemNote) {
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
        item.setItemNote(itemNote == null ? "" : itemNote.trim());
        SalesOrderItem saved = itemRepository.save(item);
        recalculateAndSave(order);
        return saved;
    }

    @Transactional
    public void deleteItem(Long salesId, Long itemId) {
        SalesOrder order = getOrder(salesId);
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿銷貨單可以刪除明細");
        SalesOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨明細"));
        if (!salesId.equals(item.getSalesId())) throw new IllegalArgumentException("明細不屬於此銷貨單");
        itemRepository.delete(item);
        recalculateAndSave(order);
    }

    private void recalculateAndSave(SalesOrder order) {
        List<SalesOrderItem> items = itemRepository.findBySalesIdOrderBySortOrderAsc(order.getSalesId());
        recalculateTotals(order, items);
        salesOrderRepository.save(order);
    }

    private void recalculateTotals(SalesOrder order, List<SalesOrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (SalesOrderItem item : items) {
            BigDecimal amount = item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
            subtotal = subtotal.add(amount);
            totalQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
        }
        BigDecimal tax = Boolean.TRUE.equals(order.getTaxEnabled())
                ? subtotal.multiply(order.getTaxRate()).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(tax));
        order.setTotalQuantity(totalQuantity);
    }


    @Transactional
    public SalesOrder returnToDraft(Long salesId) {
        SalesOrder order = getOrder(salesId);
        if (!"CONFIRMED".equals(order.getStatus())) throw new IllegalStateException("只有已確認銷貨單可以退回編輯");
        if (hasPaymentRecords(salesId) || (order.getPaidAmount() != null && order.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
            throw new IllegalStateException("此銷貨單已有收款紀錄，請先至沖帳管理刪除或調整收款後再退回編輯。");
        }

        List<SalesOrderItem> items = itemRepository.findBySalesIdOrderBySortOrderAsc(salesId);
        for (SalesOrderItem item : items) {
            if (item.getLotId() != null) {
                PurchaseLot lot = lotRepository.findById(item.getLotId()).orElse(null);
                if (lot != null) {
                    int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                    lot.setRemainingQuantity((lot.getRemainingQuantity() == null ? 0 : lot.getRemainingQuantity()) + qty);
                    if (lot.getRemainingQuantity() > 0) lot.setStatus("ACTIVE");
                    lotRepository.save(lot);
                }
            }
            movementRepository.findFirstBySourceTypeAndSourceIdAndLotIdAndMovementType("SALES", salesId, item.getLotId(), "SALES_OUT")
                    .ifPresent(movementRepository::delete);
        }

        order.setStatus("DRAFT");
        order.setPaidAmount(BigDecimal.ZERO);
        order.setUnpaidAmount(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        order.setPaymentStatus("UNPAID");
        order = salesOrderRepository.save(order);
        return order;
    }

    @Transactional
    public void deleteDraft(Long salesId) {
        SalesOrder order = getOrder(salesId);
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿單據可以刪除");
        itemRepository.findBySalesIdOrderBySortOrderAsc(salesId).forEach(itemRepository::delete);
        salesOrderRepository.delete(order);
    }

    @Transactional
    public SalesOrder voidOrder(Long salesId) {
        SalesOrder order = getOrder(salesId);
        if ("VOID".equals(order.getStatus())) return order;
        if ("DRAFT".equals(order.getStatus())) throw new IllegalStateException("草稿請使用刪除草稿，不需作廢");
        order.setStatus("VOID");
        order.setVoidedAt(LocalDateTime.now());
        order = salesOrderRepository.save(order);
        return regenerateSalesPdf(order);
    }

    @Transactional
    public SalesOrder restoreOrder(Long salesId) {
        SalesOrder order = getOrder(salesId);
        if (!"VOID".equals(order.getStatus())) return order;
        order.setStatus("CONFIRMED");
        order.setRestoredAt(LocalDateTime.now());
        order = salesOrderRepository.save(order);
        return regenerateSalesPdf(order);
    }

    @Transactional
    public SalesOrder confirm(Long salesId, String paymentType, String paymentStatus) {
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
        boolean unpaid = "UNPAID".equals(paymentStatus) || "CREDIT".equals(order.getPaymentType());
        if (unpaid) {
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

        order = regenerateSalesPdf(order);
        return order;
    }
}
