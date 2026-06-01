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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
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
    private final LabelPrintService labelPrintService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                PurchaseOrderItemRepository itemRepository,
                                PurchaseLotRepository lotRepository,
                                ProductRepository productRepository,
                                SupplierRepository supplierRepository,
                                StockMovementRepository movementRepository,
                                NumberSequenceService sequenceService,
                                PdfService pdfService,
                                CloudSyncService cloudSyncService,
                                LabelPrintService labelPrintService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.movementRepository = movementRepository;
        this.sequenceService = sequenceService;
        this.pdfService = pdfService;
        this.cloudSyncService = cloudSyncService;
        this.labelPrintService = labelPrintService;
    }

    public PurchaseOrder createDraft(Long supplierId, LocalDate purchaseDate) {
        if (supplierId == null) throw new IllegalArgumentException("供應商為必填，請雙擊查詢並選擇供應商");
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
        return purchaseOrderRepository.findAll().stream().sorted(Comparator.comparing(o -> safe(o.getPurchaseNo()))).toList();
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
    public PurchaseOrder updateDraftHeader(Long purchaseId, Long supplierId, LocalDate purchaseDate, Boolean taxEnabled, String note) {
        if (supplierId == null) throw new IllegalArgumentException("供應商為必填，請雙擊查詢並選擇供應商");
        PurchaseOrder order = getOrder(purchaseId);
        order.setSupplierId(supplierId);
        order.setPurchaseDate(purchaseDate == null ? LocalDate.now() : purchaseDate);
        order.setTaxEnabled(Boolean.TRUE.equals(taxEnabled));
        order.setNote(note == null ? "" : note.trim());
        recalculateAndSave(order);

        if ("CONFIRMED".equals(order.getStatus())) {
            syncConfirmedLotsHeader(order);
            order = regeneratePurchasePdf(order);
        }
        return order;
    }

    @Transactional
    public PurchaseOrderItem addItem(Long purchaseId,
                                     Long productId,
                                     BigDecimal wholesalePrice,
                                     BigDecimal salePrice,
                                     String trayQuantityCode,
                                     String sizeCode,
                                     Integer quantity,
                                     Long labelSettingId,
                                     String itemNote) {
        PurchaseOrder order = getOrder(purchaseId);
        if ("VOID".equals(order.getStatus())) throw new IllegalStateException("作廢單據不可新增明細，請先恢復單據");
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
        item.setLabelSettingId(labelSettingId == null ? labelPrintService.getDefaultTemplate().getSettingId() : labelSettingId);
        item.setItemNote(itemNote == null ? "" : itemNote.trim());
        PurchaseOrderItem saved = itemRepository.save(item);
        recalculateAndSave(order);
        if ("CONFIRMED".equals(order.getStatus())) {
            createLotAndMovementForConfirmedItem(order, saved);
            order = regeneratePurchasePdf(order);
        }
        return saved;
    }

    @Transactional
    public void deleteItem(Long purchaseId, Long itemId) {
        PurchaseOrder order = getOrder(purchaseId);
        if ("VOID".equals(order.getStatus())) throw new IllegalStateException("作廢單據不可刪除明細，請先恢復單據");
        PurchaseOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨明細"));
        if (!purchaseId.equals(item.getPurchaseId())) throw new IllegalArgumentException("明細不屬於此進貨單");
        if ("CONFIRMED".equals(order.getStatus())) {
            PurchaseLot lot = lotRepository.findByPurchaseItemId(itemId).orElse(null);
            if (lot != null) {
                int initial = lot.getInitialQuantity() == null ? 0 : lot.getInitialQuantity();
                int remaining = lot.getRemainingQuantity() == null ? 0 : lot.getRemainingQuantity();
                int soldQuantity = Math.max(0, initial - remaining);
                if (soldQuantity > 0) throw new IllegalArgumentException("此品項已有銷貨紀錄，不可刪除；請改為調整數量或作廢單據");
                lotRepository.delete(lot);
            }
        }
        itemRepository.delete(item);
        recalculateAndSave(order);
        if ("CONFIRMED".equals(order.getStatus())) {
            order = regeneratePurchasePdf(order);
        }
    }

    @Transactional
    public PurchaseOrderItem updateItem(Long purchaseId,
                                        Long itemId,
                                        Long productId,
                                        BigDecimal wholesalePrice,
                                        BigDecimal salePrice,
                                        String trayQuantityCode,
                                        String sizeCode,
                                        Integer quantity,
                                        Long labelSettingId,
                                        String itemNote) {
        PurchaseOrder order = getOrder(purchaseId);
        PurchaseOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("找不到進貨明細"));
        if (!purchaseId.equals(item.getPurchaseId())) throw new IllegalArgumentException("明細不屬於此進貨單");
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("進貨數量必須大於 0");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到產品資料"));

        BigDecimal unitCost = wholesalePrice == null ? BigDecimal.ZERO : wholesalePrice;
        BigDecimal unitPrice = salePrice == null ? BigDecimal.ZERO : salePrice;

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
        item.setLabelSettingId(labelSettingId == null ? labelPrintService.getDefaultTemplate().getSettingId() : labelSettingId);
        item.setItemNote(itemNote == null ? "" : itemNote.trim());
        PurchaseOrderItem saved = itemRepository.save(item);

        recalculateAndSave(order);

        if ("CONFIRMED".equals(order.getStatus())) {
            syncConfirmedLotAndMovement(order, saved);
            order = regeneratePurchasePdf(order);
        }
        return saved;
    }



    @Transactional
    public PurchaseOrder createWithFirstItem(Long supplierId,
                                             LocalDate purchaseDate,
                                             Boolean taxEnabled,
                                             String note,
                                             Long productId,
                                             BigDecimal wholesalePrice,
                                             BigDecimal salePrice,
                                             String trayQuantityCode,
                                             String sizeCode,
                                             Integer quantity,
                                             Long labelSettingId,
                                             String itemNote,
                                             boolean confirmImmediately) {
        PurchaseOrder order = createDraft(supplierId, purchaseDate);
        order.setTaxEnabled(Boolean.TRUE.equals(taxEnabled));
        order.setNote(note == null ? "" : note.trim());
        purchaseOrderRepository.save(order);
        if (productId != null) {
            addItem(order.getPurchaseId(), productId, wholesalePrice, salePrice, trayQuantityCode, sizeCode, quantity, labelSettingId, itemNote);
        }
        if (confirmImmediately) {
            return confirm(order.getPurchaseId());
        }
        return getOrder(order.getPurchaseId());
    }

    @Transactional
    public PurchaseOrder saveFullForm(Long purchaseId,
                                      Long supplierId,
                                      LocalDate purchaseDate,
                                      Boolean taxEnabled,
                                      String note,
                                      List<Long> itemIds,
                                      List<Long> productIds,
                                      List<BigDecimal> wholesalePrices,
                                      List<BigDecimal> salePrices,
                                      List<String> trayQuantityCodes,
                                      List<String> sizeCodes,
                                      List<Integer> quantities,
                                      List<Long> labelSettingIds,
                                      List<String> itemNotes,
                                      Long newProductId,
                                      BigDecimal newWholesalePrice,
                                      BigDecimal newSalePrice,
                                      String newTrayQuantityCode,
                                      String newSizeCode,
                                      Integer newQuantity,
                                      Long newLabelSettingId,
                                      String newItemNote) {
        PurchaseOrder order = updateDraftHeader(purchaseId, supplierId, purchaseDate, taxEnabled, note);
        int count = itemIds == null ? 0 : itemIds.size();
        for (int i = 0; i < count; i++) {
            Long itemId = itemIds.get(i);
            Long productId = get(productIds, i);
            BigDecimal wholesalePrice = get(wholesalePrices, i);
            BigDecimal salePrice = get(salePrices, i);
            String p = get(trayQuantityCodes, i);
            String size = get(sizeCodes, i);
            Integer qty = get(quantities, i);
            Long labelId = get(labelSettingIds, i);
            String itemNote = get(itemNotes, i);
            updateItem(purchaseId, itemId, productId, wholesalePrice, salePrice, p, size, qty, labelId, itemNote);
        }
        if (newProductId != null) {
            addItem(purchaseId, newProductId, newWholesalePrice, newSalePrice, newTrayQuantityCode, newSizeCode,
                    newQuantity == null ? 1 : newQuantity, newLabelSettingId, newItemNote);
        }
        order = getOrder(purchaseId);
        recalculateAndSave(order);
        if ("CONFIRMED".equals(order.getStatus())) {
            order = regeneratePurchasePdf(order);
        }
        return order;
    }

    private <T> T get(List<T> list, int index) {
        return list == null || index >= list.size() ? null : list.get(index);
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
            lot.setLabelSettingId(item.getLabelSettingId());
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

        order = regeneratePurchasePdf(order);
        return order;
    }

    private void createLotAndMovementForConfirmedItem(PurchaseOrder order, PurchaseOrderItem item) {
        if (lotRepository.findByPurchaseItemId(item.getItemId()).isPresent()) return;
        Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
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
        lot.setLabelSettingId(item.getLabelSettingId());
        lot.setInitialQuantity(item.getQuantity());
        lot.setRemainingQuantity(item.getQuantity());
        lot.setStatus("ACTIVE");
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

    @Transactional
    public void deleteDraft(Long purchaseId) {
        PurchaseOrder order = getOrder(purchaseId);
        if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("只有草稿單據可以刪除");
        itemRepository.findByPurchaseIdOrderBySortOrderAsc(purchaseId).forEach(itemRepository::delete);
        purchaseOrderRepository.delete(order);
    }

    @Transactional
    public PurchaseOrder voidOrder(Long purchaseId) {
        PurchaseOrder order = getOrder(purchaseId);
        if ("VOID".equals(order.getStatus())) return order;
        if ("DRAFT".equals(order.getStatus())) throw new IllegalStateException("草稿請使用刪除草稿，不需作廢");
        order.setStatus("VOID");
        order.setVoidedAt(LocalDateTime.now());
        order = purchaseOrderRepository.save(order);
        return regeneratePurchasePdf(order);
    }

    @Transactional
    public PurchaseOrder restoreOrder(Long purchaseId) {
        PurchaseOrder order = getOrder(purchaseId);
        if (!"VOID".equals(order.getStatus())) return order;
        order.setStatus("CONFIRMED");
        order.setRestoredAt(LocalDateTime.now());
        order = purchaseOrderRepository.save(order);
        return regeneratePurchasePdf(order);
    }

    private void syncConfirmedLotsHeader(PurchaseOrder order) {
        Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
        List<PurchaseLot> lots = lotRepository.findByPurchaseId(order.getPurchaseId());
        for (PurchaseLot lot : lots) {
            lot.setSupplierId(order.getSupplierId());
            lot.setSupplierCode(supplier == null ? "" : supplier.getSupplierCode());
            lot.setPurchaseDate(order.getPurchaseDate());
            lot.setPurchaseDateCode(DateUtils.toMmdd(order.getPurchaseDate()));
        }
        lotRepository.saveAll(lots);
    }

    private void syncConfirmedLotAndMovement(PurchaseOrder order, PurchaseOrderItem item) {
        PurchaseLot lot = lotRepository.findByPurchaseItemId(item.getItemId())
                .orElseThrow(() -> new IllegalStateException("找不到進貨批次，無法同步已確認明細"));

        int initial = lot.getInitialQuantity() == null ? 0 : lot.getInitialQuantity();
        int remaining = lot.getRemainingQuantity() == null ? 0 : lot.getRemainingQuantity();
        int soldQuantity = Math.max(0, initial - remaining);
        if (item.getQuantity() < soldQuantity) {
            throw new IllegalArgumentException("此批次已銷貨 " + soldQuantity + "，進貨數量不可小於已銷貨數量");
        }

        Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
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
        lot.setLabelSettingId(item.getLabelSettingId());
        lot.setInitialQuantity(item.getQuantity());
        lot.setRemainingQuantity(item.getQuantity() - soldQuantity);
        lot.setStatus(lot.getRemainingQuantity() <= 0 ? "EMPTY" : "ACTIVE");
        lotRepository.save(lot);

        movementRepository.findFirstBySourceTypeAndSourceIdAndLotIdAndMovementType("PURCHASE", order.getPurchaseId(), lot.getLotId(), "PURCHASE_IN")
                .ifPresent(movement -> {
                    movement.setProductId(item.getProductId());
                    movement.setMovementDate(order.getPurchaseDate());
                    movement.setQuantityIn(item.getQuantity());
                    movement.setUnitPrice(item.getWholesalePrice());
                    movementRepository.save(movement);
                });
    }

    private PurchaseOrder regeneratePurchasePdf(PurchaseOrder order) {
        order.setPdfUpdatedAt(LocalDateTime.now());
        order = purchaseOrderRepository.save(order);

        List<PurchaseOrderItem> items = itemRepository.findByPurchaseIdOrderBySortOrderAsc(order.getPurchaseId());
        Supplier supplier = supplierRepository.findById(order.getSupplierId() == null ? -1L : order.getSupplierId()).orElse(null);
        String pdfPath = pdfService.createPurchaseOrderPdf(order, items, supplier);
        order.setPdfPath(pdfPath);
        if (("CONFIRMED".equals(order.getStatus()) || "VOID".equals(order.getStatus())) && !lotRepository.findByPurchaseId(order.getPurchaseId()).isEmpty()) {
            order.setLabelPdfPath(labelPrintService.createPurchaseLabelsPdf(order.getPurchaseId(), 1));
        }
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

    private String safe(String value) { return value == null ? "" : value; }
}
