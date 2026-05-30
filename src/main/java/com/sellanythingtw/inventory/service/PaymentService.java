package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.PaymentRecord;
import com.sellanythingtw.inventory.entity.PaymentRecordItem;
import com.sellanythingtw.inventory.entity.SalesOrder;
import com.sellanythingtw.inventory.entity.SalesOrderItem;
import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.repository.PaymentRecordItemRepository;
import com.sellanythingtw.inventory.repository.PaymentRecordRepository;
import com.sellanythingtw.inventory.repository.SalesOrderItemRepository;
import com.sellanythingtw.inventory.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentRecordItemRepository paymentRecordItemRepository;
    private final CustomerRepository customerRepository;

    public PaymentService(SalesOrderRepository salesOrderRepository,
                          SalesOrderItemRepository salesOrderItemRepository,
                          PaymentRecordRepository paymentRecordRepository,
                          PaymentRecordItemRepository paymentRecordItemRepository,
                          CustomerRepository customerRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentRecordItemRepository = paymentRecordItemRepository;
        this.customerRepository = customerRepository;
    }

    public List<SalesOrder> listReceivables() {
        return salesOrderRepository.findByPaymentStatusInOrderBySalesDateDesc(List.of("UNPAID", "PARTIAL"));
    }

    public List<Map<String, Object>> searchReceivables(String customerCode,
                                                       String customerName,
                                                       LocalDate dateFrom,
                                                       LocalDate dateTo,
                                                       String paymentStatus,
                                                       String paymentType) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalesOrder order : salesOrderRepository.findAll()) {
            if (!"CONFIRMED".equals(order.getStatus())) continue;
            if (dateFrom != null && order.getSalesDate() != null && order.getSalesDate().isBefore(dateFrom)) continue;
            if (dateTo != null && order.getSalesDate() != null && order.getSalesDate().isAfter(dateTo)) continue;
            if (hasText(paymentStatus)) {
                BigDecimal unpaid = order.getUnpaidAmount() == null ? BigDecimal.ZERO : order.getUnpaidAmount();
                if ("PAID".equals(paymentStatus) && unpaid.compareTo(BigDecimal.ZERO) > 0) continue;
                if (("DEBT".equals(paymentStatus) || "UNPAID".equals(paymentStatus)) && unpaid.compareTo(BigDecimal.ZERO) <= 0) continue;
            }
            if (hasText(paymentType) && !paymentType.equals(order.getPaymentType())) continue;

            Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
            if (hasText(customerCode) && (customer == null || customer.getCustomerCode() == null || !customer.getCustomerCode().contains(customerCode))) continue;
            if (hasText(customerName) && (customer == null || customer.getCustomerName() == null || !customer.getCustomerName().contains(customerName))) continue;

            List<PaymentRecord> records = paymentRecordRepository.findBySalesIdOrderByPaymentDateDesc(order.getSalesId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("customer", customer);
            row.put("lastPaymentDate", records.isEmpty() ? null : records.get(0).getPaymentDate());
            row.put("paymentCount", records.size());
            rows.add(row);
        }
        rows.sort((a, b) -> {
            SalesOrder oa = (SalesOrder) a.get("order");
            SalesOrder ob = (SalesOrder) b.get("order");
            LocalDate da = oa.getSalesDate() == null ? LocalDate.MIN : oa.getSalesDate();
            LocalDate db = ob.getSalesDate() == null ? LocalDate.MIN : ob.getSalesDate();
            return db.compareTo(da);
        });
        return rows;
    }


    public Map<String, Object> receivablesSummary(List<Map<String, Object>> rows) {
        int paidOrderCount = 0;
        int unpaidOrderCount = 0;
        BigDecimal paidTotal = BigDecimal.ZERO;
        BigDecimal unpaidTotal = BigDecimal.ZERO;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                SalesOrder order = (SalesOrder) row.get("order");
                if (order == null) continue;
                BigDecimal paid = nvl(order.getPaidAmount());
                BigDecimal unpaid = nvl(order.getUnpaidAmount());
                if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    paidOrderCount++;
                    paidTotal = paidTotal.add(paid);
                }
                if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
                    unpaidOrderCount++;
                    unpaidTotal = unpaidTotal.add(unpaid);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paidOrderCount", paidOrderCount);
        result.put("paidTotal", paidTotal);
        result.put("unpaidOrderCount", unpaidOrderCount);
        result.put("unpaidTotal", unpaidTotal);
        return result;
    }

    public Map<String, Object> getPaymentDetail(Long salesId) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        Customer customer = customerRepository.findById(order.getCustomerId() == null ? -1L : order.getCustomerId()).orElse(null);
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesIdOrderBySortOrderAsc(salesId);
        List<PaymentRecord> records = paymentRecordRepository.findBySalesIdOrderByPaymentDateDesc(salesId);
        List<PaymentRecordItem> paidItems = paymentRecordItemRepository.findBySalesId(salesId);

        Map<Long, Integer> paidQtyByItem = new LinkedHashMap<>();
        Map<Long, BigDecimal> paidAmountByItem = new LinkedHashMap<>();
        for (PaymentRecordItem pi : paidItems) {
            Long itemId = pi.getSalesItemId();
            if (itemId == null) continue;
            paidQtyByItem.put(itemId, paidQtyByItem.getOrDefault(itemId, 0) + nvl(pi.getReceivedQuantity()));
            paidAmountByItem.put(itemId, paidAmountByItem.getOrDefault(itemId, BigDecimal.ZERO).add(nvl(pi.getAmount())));
        }

        List<Map<String, Object>> itemRows = new ArrayList<>();
        for (SalesOrderItem item : items) {
            int soldQty = nvl(item.getQuantity());
            int paidQty = paidQtyByItem.getOrDefault(item.getItemId(), 0);
            int unpaidQty = Math.max(0, soldQty - paidQty);
            BigDecimal itemAmount = nvl(item.getAmount());
            BigDecimal paidAmount = paidAmountByItem.getOrDefault(item.getItemId(), BigDecimal.ZERO);
            BigDecimal unpaidAmount = itemAmount.subtract(paidAmount);
            if (unpaidAmount.compareTo(BigDecimal.ZERO) < 0) unpaidAmount = BigDecimal.ZERO;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item", item);
            row.put("paidQuantity", paidQty);
            row.put("unpaidQuantity", unpaidQty);
            row.put("paidAmount", paidAmount);
            row.put("unpaidAmount", unpaidAmount);
            itemRows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> recordBlocks = new ArrayList<>();
        for (PaymentRecord record : records) {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("record", record);
            block.put("items", paymentRecordItemRepository.findByPaymentIdOrderByPaymentItemIdAsc(record.getPaymentId()));
            recordBlocks.add(block);
        }

        result.put("order", order);
        result.put("customer", customer);
        result.put("items", itemRows);
        result.put("records", records);
        result.put("recordBlocks", recordBlocks);
        return result;
    }



    @Transactional
    public void deletePaymentRecord(Long salesId, Long paymentId) {
        PaymentRecord record = paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("找不到收款紀錄"));
        if (!salesId.equals(record.getSalesId())) throw new IllegalArgumentException("收款紀錄不屬於此銷貨單");
        for (PaymentRecordItem item : paymentRecordItemRepository.findByPaymentIdOrderByPaymentItemIdAsc(paymentId)) {
            paymentRecordItemRepository.delete(item);
        }
        paymentRecordRepository.delete(record);
        recalculateSalesPaymentStatus(salesId);
    }

    @Transactional
    public PaymentRecord updatePaymentRecordItems(Long salesId,
                                                  Long paymentId,
                                                  String method,
                                                  LocalDate paymentDate,
                                                  String note,
                                                  List<Long> paymentItemIds,
                                                  List<Integer> receivedQuantities,
                                                  List<BigDecimal> receivedAmounts) {
        PaymentRecord record = paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("找不到收款紀錄"));
        if (!salesId.equals(record.getSalesId())) throw new IllegalArgumentException("收款紀錄不屬於此銷貨單");

        List<PaymentRecordItem> currentItems = paymentRecordItemRepository.findByPaymentIdOrderByPaymentItemIdAsc(paymentId);
        Map<Long, PaymentRecordItem> byId = new LinkedHashMap<>();
        for (PaymentRecordItem item : currentItems) byId.put(item.getPaymentItemId(), item);

        BigDecimal total = BigDecimal.ZERO;
        if (paymentItemIds != null) {
            for (int i = 0; i < paymentItemIds.size(); i++) {
                Long paymentItemId = paymentItemIds.get(i);
                PaymentRecordItem pi = byId.get(paymentItemId);
                if (pi == null) continue;

                int requestedQty = receivedQuantities != null && receivedQuantities.size() > i && receivedQuantities.get(i) != null ? receivedQuantities.get(i) : 0;
                BigDecimal requestedAmount = receivedAmounts != null && receivedAmounts.size() > i && receivedAmounts.get(i) != null ? receivedAmounts.get(i) : BigDecimal.ZERO;

                validateEditedPaymentItem(salesId, paymentId, pi, requestedQty, requestedAmount);
                pi.setReceivedQuantity(requestedQty);
                pi.setAmount(requestedAmount);
                pi.setNote(note);
                paymentRecordItemRepository.save(pi);
                total = total.add(requestedAmount);
            }
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            deletePaymentRecord(salesId, paymentId);
            return record;
        }
        record.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
        record.setPaymentMethod(hasText(method) ? method : record.getPaymentMethod());
        record.setNote(note);
        record.setAmount(total);
        record = paymentRecordRepository.save(record);
        recalculateSalesPaymentStatus(salesId);
        return record;
    }

    private void validateEditedPaymentItem(Long salesId, Long editingPaymentId, PaymentRecordItem editingItem, int requestedQty, BigDecimal requestedAmount) {
        if (requestedQty < 0) throw new IllegalArgumentException("收款數量不可小於 0");
        if (requestedAmount == null) requestedAmount = BigDecimal.ZERO;
        if (requestedAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("收款金額不可小於 0");

        SalesOrderItem salesItem = salesOrderItemRepository.findById(editingItem.getSalesItemId())
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨品項"));

        int otherPaidQty = 0;
        BigDecimal otherPaidAmount = BigDecimal.ZERO;
        for (PaymentRecordItem pi : paymentRecordItemRepository.findBySalesItemId(salesItem.getItemId())) {
            if (editingPaymentId.equals(pi.getPaymentId())) continue;
            otherPaidQty += nvl(pi.getReceivedQuantity());
            otherPaidAmount = otherPaidAmount.add(nvl(pi.getAmount()));
        }

        int maxQty = nvl(salesItem.getQuantity()) - otherPaidQty;
        BigDecimal maxAmount = nvl(salesItem.getAmount()).subtract(otherPaidAmount);
        if (maxAmount.compareTo(BigDecimal.ZERO) < 0) maxAmount = BigDecimal.ZERO;

        if (requestedQty > maxQty) throw new IllegalArgumentException("品項 " + salesItem.getProductName() + " 收款數量不可大於剩餘可收數量");
        if (requestedAmount.compareTo(maxAmount) > 0) throw new IllegalArgumentException("品項 " + salesItem.getProductName() + " 收款金額不可大於剩餘可收金額");
    }

    private void recalculateSalesPaymentStatus(Long salesId) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        BigDecimal paid = BigDecimal.ZERO;
        for (PaymentRecord r : paymentRecordRepository.findBySalesIdOrderByPaymentDateDesc(salesId)) {
            paid = paid.add(nvl(r.getAmount()));
        }
        if (paid.compareTo(nvl(order.getTotalAmount())) > 0) paid = nvl(order.getTotalAmount());
        order.setPaidAmount(paid);
        order.setUnpaidAmount(nvl(order.getTotalAmount()).subtract(paid));
        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setUnpaidAmount(BigDecimal.ZERO);
            order.setPaymentStatus("PAID");
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus("PARTIAL");
        } else {
            order.setPaymentStatus("UNPAID");
        }
        salesOrderRepository.save(order);
    }

    @Transactional
    public PaymentRecord receivePaymentByItems(Long salesId,
                                               String method,
                                               LocalDate paymentDate,
                                               String note,
                                               List<Long> salesItemIds,
                                               List<Integer> receivedQuantities,
                                               List<BigDecimal> receivedAmounts) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        if (!"CONFIRMED".equals(order.getStatus())) throw new IllegalArgumentException("只有已確認銷貨單可以沖帳");
        if (salesItemIds == null || salesItemIds.isEmpty()) throw new IllegalArgumentException("請至少選擇一筆收款品項");

        PaymentRecord record = new PaymentRecord();
        record.setSalesId(order.getSalesId());
        record.setSalesNo(order.getSalesNo());
        record.setCustomerId(order.getCustomerId());
        record.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
        record.setPaymentMethod(hasText(method) ? method : "CASH");
        record.setNote(note);
        record.setReceiptNo("RC" + System.currentTimeMillis());
        record.setAmount(BigDecimal.ZERO);
        record = paymentRecordRepository.save(record);

        BigDecimal totalReceived = BigDecimal.ZERO;
        for (int i = 0; i < salesItemIds.size(); i++) {
            Long itemId = salesItemIds.get(i);
            if (itemId == null) continue;
            SalesOrderItem item = salesOrderItemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("找不到銷貨品項"));
            if (!salesId.equals(item.getSalesId())) throw new IllegalArgumentException("收款品項不屬於此銷貨單");

            int requestedQty = receivedQuantities != null && receivedQuantities.size() > i && receivedQuantities.get(i) != null ? receivedQuantities.get(i) : 0;
            BigDecimal requestedAmount = receivedAmounts != null && receivedAmounts.size() > i && receivedAmounts.get(i) != null ? receivedAmounts.get(i) : BigDecimal.ZERO;
            if (requestedQty <= 0 && requestedAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            Map<String, Object> detail = getPaymentDetail(salesId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) detail.get("items");
            int unpaidQty = 0;
            BigDecimal unpaidAmount = BigDecimal.ZERO;
            for (Map<String, Object> row : rows) {
                SalesOrderItem rowItem = (SalesOrderItem) row.get("item");
                if (itemId.equals(rowItem.getItemId())) {
                    unpaidQty = (Integer) row.get("unpaidQuantity");
                    unpaidAmount = (BigDecimal) row.get("unpaidAmount");
                    break;
                }
            }

            if (requestedQty > unpaidQty) throw new IllegalArgumentException("品項 " + item.getProductName() + " 收款數量不可大於未收數量");
            BigDecimal amount = requestedAmount.compareTo(BigDecimal.ZERO) > 0
                    ? requestedAmount
                    : nvl(item.getUnitPrice()).multiply(BigDecimal.valueOf(requestedQty));
            if (amount.compareTo(unpaidAmount) > 0) throw new IllegalArgumentException("品項 " + item.getProductName() + " 收款金額不可大於未收金額");

            PaymentRecordItem pi = new PaymentRecordItem();
            pi.setPaymentId(record.getPaymentId());
            pi.setSalesId(order.getSalesId());
            pi.setSalesItemId(item.getItemId());
            pi.setProductId(item.getProductId());
            pi.setProductCode(item.getProductCode());
            pi.setProductName(item.getProductName());
            pi.setProductAlias(item.getProductAlias());
            pi.setUnitPrice(item.getUnitPrice());
            pi.setSalesQuantity(item.getQuantity());
            pi.setReceivedQuantity(requestedQty);
            pi.setAmount(amount);
            pi.setNote(note);
            paymentRecordItemRepository.save(pi);
            totalReceived = totalReceived.add(amount);
        }

        if (totalReceived.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("收款金額必須大於 0");
        record.setAmount(totalReceived);
        record = paymentRecordRepository.save(record);

        BigDecimal paid = nvl(order.getPaidAmount()).add(totalReceived);
        if (paid.compareTo(nvl(order.getTotalAmount())) > 0) paid = nvl(order.getTotalAmount());
        order.setPaidAmount(paid);
        order.setUnpaidAmount(nvl(order.getTotalAmount()).subtract(paid));
        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setUnpaidAmount(BigDecimal.ZERO);
            order.setPaymentStatus("PAID");
        } else {
            order.setPaymentStatus("PARTIAL");
        }
        order.setPaymentType(record.getPaymentMethod());
        salesOrderRepository.save(order);
        return record;
    }

    @Transactional
    public PaymentRecord receivePayment(Long salesId, String method, BigDecimal amount, LocalDate paymentDate, String note) {
        SalesOrder order = salesOrderRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("找不到銷貨單"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("收款金額必須大於 0");
        if (order.getUnpaidAmount() == null || order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("此銷貨單沒有未收款");
        if (amount.compareTo(order.getUnpaidAmount()) > 0) throw new IllegalArgumentException("收款金額不可大於未收金額");

        PaymentRecord record = new PaymentRecord();
        record.setSalesId(order.getSalesId());
        record.setSalesNo(order.getSalesNo());
        record.setCustomerId(order.getCustomerId());
        record.setReceiptNo("RC" + System.currentTimeMillis());
        record.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
        record.setPaymentMethod(hasText(method) ? method : "CASH");
        record.setAmount(amount);
        record.setNote(note);
        paymentRecordRepository.save(record);

        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        order.setPaidAmount(paid.add(amount));
        order.setUnpaidAmount(order.getTotalAmount().subtract(order.getPaidAmount()));
        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setUnpaidAmount(BigDecimal.ZERO);
            order.setPaymentStatus("PAID");
            order.setPaymentType(record.getPaymentMethod());
        } else {
            order.setPaymentStatus("PARTIAL");
        }
        salesOrderRepository.save(order);
        return record;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int nvl(Integer value) { return value == null ? 0 : value; }
    private BigDecimal nvl(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
