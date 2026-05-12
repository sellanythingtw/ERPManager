package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(ProductRepository productRepository, StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<Map<String, Object>> getRealtimeInventory() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            int quantity = stockMovementRepository.getProductQuantity(product.getProductId());
            int safety = product.getSafetyStock() == null ? 0 : product.getSafetyStock();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", product.getProductId());
            row.put("productCode", product.getProductCode());
            row.put("category", product.getCategory());
            row.put("productName", product.getProductName());
            row.put("productAlias", product.getProductAlias());
            row.put("specification", product.getSpecification());
            row.put("unit", product.getUnit());
            row.put("quantity", quantity);
            row.put("safetyStock", safety);
            row.put("stockLevel", quantity - safety);
            row.put("status", quantity <= 0 ? "OUT" : quantity <= safety ? "LOW" : "NORMAL");
            result.add(row);
        }
        return result;
    }
}
