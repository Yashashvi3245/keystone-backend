package com.keystone.controller;

import com.keystone.model.Customer;
import com.keystone.model.User;
import com.keystone.repository.UserRepository;
import com.keystone.service.CustomerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

    public CustomerController(
            CustomerService customerService,
            UserRepository userRepository) {

        this.customerService = customerService;
        this.userRepository = userRepository;
    }

    // =========================
    // GET ALL CUSTOMERS
    // MANAGER / DISPATCHER ONLY
    // =========================

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<Customer>> getAllCustomers() {

        return ResponseEntity.ok(
                customerService.getAllCustomers()
        );
    }

    // =========================
    // GET CUSTOMER BY ID
    // MANAGER / DISPATCHER
    // OR CUSTOMER'S OWN CUSTOMER
    // =========================

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCustomerById(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        User currentUser =
                userRepository
                        .findByEmail(authentication.getName())
                        .orElse(null);

        if (currentUser == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        // Manager and Dispatcher can view any customer
        if (currentUser.getRole().name().equals("MANAGER")
                || currentUser.getRole().name().equals("DISPATCHER")) {

            return customerService
                    .getCustomerById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        // Customer can only view their own customer record
        if (currentUser.getRole().name().equals("CUSTOMER")) {

            if (currentUser.getCustomer() == null) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("Customer account is not linked to an organization");
            }

            if (!currentUser.getCustomer().getId().equals(id)) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You can only access your own customer organization");
            }

            return customerService
                    .getCustomerById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("You do not have permission to access this customer");
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

        return customerService
                .getCustomerById(id)
                .map(existingCustomer -> {

                    existingCustomer.setCompanyName(
                            updatedCustomer.getCompanyName()
                    );

                    existingCustomer.setContactEmail(
                            updatedCustomer.getContactEmail()
                    );

                    Customer savedCustomer =
                            customerService.saveCustomer(
                                    existingCustomer
                            );

                    return ResponseEntity.ok(savedCustomer);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // DELETE CUSTOMER
    // MANAGER ONLY (brief: only manager can delete)
    // =========================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id) {

        if (customerService
                .getCustomerById(id)
                .isEmpty()) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        customerService.deleteCustomer(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}