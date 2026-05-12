package com.sellanythingtw.inventory.service;

import com.sellanythingtw.inventory.entity.Customer;
import com.sellanythingtw.inventory.entity.Product;
import com.sellanythingtw.inventory.entity.Supplier;
import com.sellanythingtw.inventory.repository.CustomerRepository;
import com.sellanythingtw.inventory.repository.ProductRepository;
import com.sellanythingtw.inventory.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MasterDataService {
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final NumberSequenceService sequenceService;

    public MasterDataService(ProductRepository productRepository, CustomerRepository customerRepository,
                             SupplierRepository supplierRepository, NumberSequenceService sequenceService) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.sequenceService = sequenceService;
    }

    public List<Product> listProducts() { return productRepository.findAll(); }
    public List<Customer> listCustomers() { return customerRepository.findAll(); }
    public List<Supplier> listSuppliers() { return supplierRepository.findAll(); }

    public Product saveProduct(Product product) {
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(sequenceService.nextMasterCode("P"));
        }
        return productRepository.save(product);
    }

    public Customer saveCustomer(Customer customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            customer.setCustomerCode(sequenceService.nextMasterCode("C"));
        }
        return customerRepository.save(customer);
    }

    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().isBlank()) {
            supplier.setSupplierCode(sequenceService.nextMasterCode("S"));
        }
        return supplierRepository.save(supplier);
    }
}
