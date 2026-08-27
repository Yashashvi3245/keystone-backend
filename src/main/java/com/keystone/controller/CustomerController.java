package com.keystone.controller;

import com.keystone.model.Customer;
import com.keystone.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // =========================
    // GET ALL CUSTOMERS
    // =========================
    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    // =========================
    // GET CUSTOMER BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(
            @PathVariable Long id) {

        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // CREATE CUSTOMER
    // MANAGER / DISPATCHER ONLY
    // =========================
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Customer> createCustomer(
            @Valid @RequestBody Customer customer) {

        Customer savedCustomer =
                customerService.saveCustomer(customer);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedCustomer);
    }

    // =========================
    // UPDATE CUSTOMER
    // MANAGER / DISPATCHER ONLY
    // =========================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody Customer updatedCustomer) {

        return customerService.getCustomerById(id)
                .map(existingCustomer -> {

                    existingCustomer.setCompanyName(
                            updatedCustomer.getCompanyName()
                    );

                    existingCustomer.setContactEmail(
                            updatedCustomer.getContactEmail()
                    );

                    Customer savedCustomer =
                            customerService.saveCustomer(existingCustomer);

                    return ResponseEntity.ok(savedCustomer);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // DELETE CUSTOMER
    // MANAGER / DISPATCHER ONLY
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id) {

        if (customerService.getCustomerById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        customerService.deleteCustomer(id);

        return ResponseEntity.noContent().build();
    }
}