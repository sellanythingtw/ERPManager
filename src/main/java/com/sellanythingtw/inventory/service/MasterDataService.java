package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.Supplier;
import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MasterDataService {
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final NumberSequenceService sequenceService;

    public MasterDataService(ProductRepository productRepository,
                             CustomerRepository customerRepository,
                             SupplierRepository supplierRepository,
                             NumberSequenceService sequenceService) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.sequenceService = sequenceService;
    }

    public List<Product> listProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(p -> safe(p.getProductCode())))
                .toList();
    }

    public List<Customer> listCustomers() {
        return customerRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> safe(c.getCustomerCode())))
                .toList();
    }

    public List<Supplier> listSuppliers() {
        return supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> safe(s.getSupplierCode())))
                .toList();
    }

    public Product saveProduct(Product product) {
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(sequenceService.nextMasterCode("P"));
        }
        if (product.getActive() == null) {
            if (product.getProductId() != null) {
                productRepository.findById(product.getProductId()).ifPresent(existing -> product.setActive(existing.getActive() == null || existing.getActive()));
            } else {
                product.setActive(true);
            }
        }
        return productRepository.save(product);
    }

    public Customer saveCustomer(Customer customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            customer.setCustomerCode(sequenceService.nextMasterCode("C"));
        }
        if (customer.getActive() == null) {
            if (customer.getCustomerId() != null) {
                customerRepository.findById(customer.getCustomerId()).ifPresent(existing -> customer.setActive(existing.getActive() == null || existing.getActive()));
            } else {
                customer.setActive(true);
            }
        }
        return customerRepository.save(customer);
    }

    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().isBlank()) {
            supplier.setSupplierCode(sequenceService.nextMasterCode("S"));
        }
        if (supplier.getActive() == null) {
            if (supplier.getSupplierId() != null) {
                supplierRepository.findById(supplier.getSupplierId()).ifPresent(existing -> supplier.setActive(existing.getActive() == null || existing.getActive()));
            } else {
                supplier.setActive(true);
            }
        }
        return supplierRepository.save(supplier);
    }

    public Product voidProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("找不到產品"));
        product.setActive(false);
        return productRepository.save(product);
    }

    public Product restoreProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("找不到產品"));
        product.setActive(true);
        return productRepository.save(product);
    }

    public Customer voidCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("找不到客戶"));
        customer.setActive(false);
        return customerRepository.save(customer);
    }

    public Customer restoreCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("找不到客戶"));
        customer.setActive(true);
        return customerRepository.save(customer);
    }

    public Supplier voidSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow(() -> new IllegalArgumentException("找不到供應商"));
        supplier.setActive(false);
        return supplierRepository.save(supplier);
    }

    public Supplier restoreSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow(() -> new IllegalArgumentException("找不到供應商"));
        supplier.setActive(true);
        return supplierRepository.save(supplier);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
