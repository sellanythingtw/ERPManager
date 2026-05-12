package com.gigastone.inventory.service;

import com.gigastone.inventory.entity.*;
import com.gigastone.inventory.repository.*;
import com.gigastone.inventory.utils.BarcodeUtils;
import com.gigastone.inventory.utils.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseOrderService {
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final PurchaseLotRepository lotRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository movementRepository;
    private final NumberSequenceService sequenceService;
    private final PdfService pdfService;
    private final CloudSyncService cloudSyncService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                PurchaseOrderItemRepository itemRepository,
                                PurchaseLotRepository lotRepository,
                                SupplierRepository supplierRepository,
                                StockMovementRepository movementRepository,
                                NumberSequenceService sequenceService,
                                PdfService pdfService,
                                CloudSyncService cloudSyncService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
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
        return purchaseOrderRepository.save(order);
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
            BigDecimal amount = item.getWholesalePrice().multiply(BigDecimal.valueOf(item.getQuantity()));
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

        Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
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

    public List<PurchaseLot> listLots(Long purchaseId) {
        return lotRepository.findByPurchaseId(purchaseId);
    }
}
