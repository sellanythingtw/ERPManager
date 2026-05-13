package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.*;
import com.sellanythingtw.inventory.repository.*;
import com.sellanythingtw.inventory.utils.BarcodeUtils;
import com.sellanythingtw.inventory.utils.DateUtils;
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
public class PurchaseOrderService {
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final PurchaseLotRepository lotRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository movementRepository;
    private final NumberSequenceService sequenceService;
    private final PdfService pdfService;
    private final CloudSyncService cloudSyncService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                PurchaseOrderItemRepository itemRepository,
                                PurchaseLotRepository lotRepository,
                                ProductRepository productRepository,
                                SupplierRepository supplierRepository,
                                StockMovementRepository movementRepository,
                                NumberSequenceService sequenceService,
                                PdfService pdfService,
                                CloudSyncService cloudSyncService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.movementRepository = movementRepository;
        this.sequenceService = sequenceService;
        this.pdfService = pdfService;
        this.cloudSyncService = cloudSyncService;
    }

    public PurchaseOrder createDraft(Long supplierId, LocalDate purchaseDate) {
        LocalDate today = LocalDate.now();
        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseNo(sequenceService.nextDocumentNo("PI", today.format(YMD)));
        order.setDocumentDate(today);
        order.setPurchaseDate(purchaseDate == null ? today : purchaseDate);
        order.setSupplierId(supplierId);
        order.setStatus("DRAFT");
        recalculateTotals(order, List.of());
        return purchaseOrderRepository.save(order);
    }

    public List<PurchaseOrder> listAll() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public PurchaseOrder getOrder(Long purchaseId) {
        return purchaseOrderRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨單"));
    }

    public List<PurchaseOrderItem> listItems(Long purchaseId) {
        return itemRepository.findByPurchaseIdOrderBySortOrderAsc(purchaseId);
    }

    public Map<String, Object> getDetail(Long purchaseId) {
        Map<String, Object> result = new LinkedHashMap<>();
        PurchaseOrder order = getOrder(purchaseId);
        result.put("order", order);
        result.put("items", listItems(purchaseId));
        result.put("lots", lotRepository.findByPurchaseId(purchaseId));
        supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId())
                .ifPresent(supplier -> result.put("supplier", supplier));
        return result;
    }

    @Transactional
    public PurchaseOrderItem addItem(Long purchaseId,
                                     Long productId,
                                     BigDecimal wholesalePrice,
                                     BigDecimal salePrice,
                                     String trayQuantityCode,
                                     String sizeCode,
                                     Integer quantity) {
        PurchaseOrder order = getOrder(purchaseId);
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿進貨單可以新增明細");
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("進貨數量必須大於 0");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到產品資料"));

        BigDecimal unitCost = wholesalePrice == null ? BigDecimal.ZERO : wholesalePrice;
        BigDecimal unitPrice = salePrice == null ? BigDecimal.ZERO : salePrice;

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseId(purchaseId);
        item.setProductId(product.getProductId());
        item.setProductCode(product.getProductCode());
        item.setProductName(product.getProductName());
        item.setProductAlias(product.getProductAlias());
        item.setSpecification(product.getSpecification());
        item.setColor(product.getColor());
        item.setUnit(product.getUnit());
        item.setWholesalePrice(unitCost);
        item.setSalePrice(unitPrice);
        item.setTrayQuantityCode(trayQuantityCode == null ? "" : trayQuantityCode.trim());
        item.setSizeCode(sizeCode == null ? "" : sizeCode.trim());
        item.setQuantity(quantity);
        item.setAmount(unitCost.multiply(BigDecimal.valueOf(quantity)));
        item.setSortOrder(itemRepository.findByPurchaseIdOrderBySortOrderAsc(purchaseId).size() + 1);
        PurchaseOrderItem saved = itemRepository.save(item);
        recalculateAndSave(order);
        return saved;
    }

    @Transactional
    public void deleteItem(Long purchaseId, Long itemId) {
        PurchaseOrder order = getOrder(purchaseId);
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿進貨單可以刪除明細");
        PurchaseOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨明細"));
        if (!purchaseId.equals(item.getPurchaseId())) throw new IllegalArgumentException("明細不屬於此進貨單");
        itemRepository.delete(item);
        recalculateAndSave(order);
    }

    @Transactional
    public PurchaseOrder confirm(Long purchaseId) {
        PurchaseOrder order = purchaseOrderRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨單"));
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿進貨單可以確認");
        List<PurchaseOrderItem> items = itemRepository.findByPurchaseIdOrderBySortOrderAsc(purchaseId);
        if (items.isEmpty()) throw new IllegalStateException("進貨單沒有明細");

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (PurchaseOrderItem item : items) {
            BigDecimal price = item.getWholesalePrice() == null ? BigDecimal.ZERO : item.getWholesalePrice();
            BigDecimal amount = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setAmount(amount);
            subtotal = subtotal.add(amount);
            totalQuantity += item.getQuantity();
        }
        itemRepository.saveAll(items);

        BigDecimal tax = Boolean.TRUE.equals(order.getTaxEnabled())
                ? subtotal.multiply(order.getTaxRate()).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(tax));
        order.setTotalQuantity(totalQuantity);
        order.setStatus("CONFIRMED");
        order = purchaseOrderRepository.save(order);

        Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
        for (PurchaseOrderItem item : items) {
            long lotSeq = sequenceService.nextValue("LOT");
            String barcodeValue = BarcodeUtils.createLotBarcode(order.getPurchaseDate(), lotSeq);

            PurchaseLot lot = new PurchaseLot();
            lot.setPurchaseId(order.getPurchaseId());
            lot.setPurchaseItemId(item.getItemId());
            lot.setLotNo("LOT" + String.format("%08d", lotSeq));
            lot.setBarcodeValue(barcodeValue);
            lot.setProductId(item.getProductId());
            lot.setProductCode(item.getProductCode());
            lot.setProductName(item.getProductName());
            lot.setProductAlias(item.getProductAlias());
            lot.setSupplierId(order.getSupplierId());
            lot.setSupplierCode(supplier == null ? "" : supplier.getSupplierCode());
            lot.setPurchaseDate(order.getPurchaseDate());
            lot.setPurchaseDateCode(DateUtils.toMmdd(order.getPurchaseDate()));
            lot.setWholesalePrice(item.getWholesalePrice());
            lot.setSalePrice(item.getSalePrice());
            lot.setTrayQuantityCode(item.getTrayQuantityCode());
            lot.setSizeCode(item.getSizeCode());
            lot.setInitialQuantity(item.getQuantity());
            lot.setRemainingQuantity(item.getQuantity());
            lotRepository.save(lot);

            StockMovement movement = new StockMovement();
            movement.setProductId(item.getProductId());
            movement.setLotId(lot.getLotId());
            movement.setBarcodeValue(barcodeValue);
            movement.setMovementType("PURCHASE_IN");
            movement.setSourceType("PURCHASE");
            movement.setSourceId(order.getPurchaseId());
            movement.setSourceNo(order.getPurchaseNo());
            movement.setMovementDate(order.getPurchaseDate());
            movement.setQuantityIn(item.getQuantity());
            movement.setQuantityOut(0);
            movement.setUnitPrice(item.getWholesalePrice());
            movementRepository.save(movement);
        }

        String pdfPath = pdfService.createPurchaseOrderPdf(order, items, supplier);
        order.setPdfPath(pdfPath);
        order = purchaseOrderRepository.save(order);
        cloudSyncService.uploadPdfIfEnabled("PURCHASE", order.getPurchaseId(), pdfPath, supplier == null ? "未指定供應商" : supplier.getSupplierName());
        return order;
    }

    private void recalculateAndSave(PurchaseOrder order) {
        List<PurchaseOrderItem> items = itemRepository.findByPurchaseIdOrderBySortOrderAsc(order.getPurchaseId());
        recalculateTotals(order, items);
        purchaseOrderRepository.save(order);
    }

    private void recalculateTotals(PurchaseOrder order, List<PurchaseOrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (PurchaseOrderItem item : items) {
            BigDecimal price = item.getWholesalePrice() == null ? BigDecimal.ZERO : item.getWholesalePrice();
            Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(qty)));
            totalQuantity += qty;
        }
        BigDecimal tax = Boolean.TRUE.equals(order.getTaxEnabled())
                ? subtotal.multiply(order.getTaxRate()).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(tax));
        order.setTotalQuantity(totalQuantity);
    }

    public List<PurchaseLot> listLots(Long purchaseId) {
        return lotRepository.findByPurchaseId(purchaseId);
    }
}
