package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.PurchaseLot;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.PurchaseLotRepository;
import com.sellanythingtw.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    private final ProductRepository productRepository;
    private final PurchaseLotRepository purchaseLotRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(ProductRepository productRepository,
                            PurchaseLotRepository purchaseLotRepository,
                            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.purchaseLotRepository = purchaseLotRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<Map<String, Object>> getRealtimeInventory() {
        return searchRealtimeInventory(null, null, null, null, null);
    }

    public List<Map<String, Object>> searchRealtimeInventory(String category, String productName) {
        return searchRealtimeInventory(category, productName, null, null, null);
    }

    public List<Map<String, Object>> searchRealtimeInventory(String category, String productName, String color, String stockFilter, String statusFilter) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            if (hasText(category) && (product.getCategory() == null || !product.getCategory().contains(category.trim()))) continue;
            if (hasText(productName)) {
                String key = productName.trim();
                String nameText = safe(product.getProductCode()) + " " + safe(product.getProductName()) + " " + safe(product.getProductAlias());
                if (!nameText.contains(key)) continue;
            }
            if (hasText(color) && !safe(product.getColor()).contains(color.trim())) continue;

            int quantity = stockMovementRepository.getProductQuantity(product.getProductId());
            int safety = product.getSafetyStock() == null ? 0 : product.getSafetyStock();
            boolean low = quantity <= safety;
            if (hasText(stockFilter)) {
                if ("ZERO".equals(stockFilter) && quantity != 0) continue;
                if ("POSITIVE".equals(stockFilter) && quantity <= 0) continue;
                if ("LOW".equals(stockFilter) && !low) continue;
            }
            if (hasText(statusFilter)) {
                String status = low ? "LOW" : "NORMAL";
                if (!statusFilter.equals(status)) continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", product.getProductId());
            row.put("productCode", product.getProductCode());
            row.put("category", product.getCategory());
            row.put("productName", product.getProductName());
            row.put("productAlias", product.getProductAlias());
            row.put("specification", product.getSpecification());
            row.put("unit", product.getUnit());
            row.put("defaultPurchasePrice", product.getDefaultPurchasePrice());
            row.put("defaultSalePrice", product.getDefaultSalePrice());
            row.put("note", product.getNote());
            row.put("activeText", product.getActive() == null || product.getActive() ? "正常" : "已作廢");
            row.put("quantity", quantity);
            row.put("safetyStock", safety);
            row.put("stockLevel", quantity - safety);
            row.put("status", low ? "LOW" : "NORMAL");
            row.put("statusText", low ? "庫存水位低" : "庫存充足");
            row.put("lowStock", low);
            row.put("rowBackground", colorToBackground(product.getColor()));
            row.put("color", product.getColor());
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getOpenLots(Long productId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到產品"));
        List<PurchaseLot> lots = purchaseLotRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByPurchaseDateDesc(productId, 0);
        for (PurchaseLot lot : lots) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product", product);
            row.put("lot", lot);
            row.put("usedQuantity", (lot.getInitialQuantity() == null ? 0 : lot.getInitialQuantity()) - (lot.getRemainingQuantity() == null ? 0 : lot.getRemainingQuantity()));
            result.add(row);
        }
        return result;
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到產品"));
    }

    public String toCsv(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("品號,類別,品名,小名,規格,顏色,單位,預設進貨價,預設銷貨價,備註,產品狀態,庫存,安全庫存,狀況\n");
        for (Map<String, Object> r : rows) {
            sb.append(csv(r.get("productCode"))).append(',')
              .append(csv(r.get("category"))).append(',')
              .append(csv(r.get("productName"))).append(',')
              .append(csv(r.get("productAlias"))).append(',')
              .append(csv(r.get("specification"))).append(',')
              .append(csv(r.get("color"))).append(',')
              .append(csv(r.get("unit"))).append(',')
              .append(csv(r.get("defaultPurchasePrice"))).append(',')
              .append(csv(r.get("defaultSalePrice"))).append(',')
              .append(csv(r.get("note"))).append(',')
              .append(csv(r.get("activeText"))).append(',')
              .append(csv(r.get("quantity"))).append(',')
              .append(csv(r.get("safetyStock"))).append(',')
              .append(csv(r.get("statusText"))).append('\n');
        }
        return sb.toString();
    }

    private String colorToBackground(String color) {
        if (!hasText(color)) return "";
        String c = color.trim();
        if (c.startsWith("#")) return c;
        String lower = c.toLowerCase();
        return switch (lower) {
            case "紅", "紅色", "red" -> "#fee2e2";
            case "粉", "粉色", "pink" -> "#fce7f3";
            case "橘", "橘色", "orange" -> "#ffedd5";
            case "黃", "黃色", "yellow" -> "#fef9c3";
            case "綠", "綠色", "green" -> "#dcfce7";
            case "藍", "藍色", "blue" -> "#dbeafe";
            case "紫", "紫色", "purple" -> "#ede9fe";
            case "灰", "灰色", "gray", "grey" -> "#f3f4f6";
            case "白", "白色", "white" -> "";
            default -> c;
        };
    }

    private String csv(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
