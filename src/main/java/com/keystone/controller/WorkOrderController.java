package com.keystone.controller;

import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    // =========================
    // GET ALL WORK ORDERS
    // MANAGER / DISPATCHER / TECHNICIAN
    // =========================
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public List<WorkOrderResponse> getAllWorkOrders() {
        return workOrderService.getAllWorkOrders();
    }

    // =========================
    // GET WORK ORDER BY ID
    // MANAGER / DISPATCHER / TECHNICIAN
    // =========================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderById(
            @PathVariable Long id) {

        try {
            return ResponseEntity.ok(
                    workOrderService.getWorkOrderById(id)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================
    // GET WORK ORDER BY CODE
    // MANAGER / DISPATCHER / TECHNICIAN
    // =========================
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderByCode(
            @PathVariable String code) {

        try {
            return ResponseEntity.ok(
                    workOrderService.getWorkOrderByCode(code)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================
    // CREATE WORK ORDER
    // MANAGER / DISPATCHER ONLY
    // =========================
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<WorkOrderResponse> createWorkOrder(
            @Valid @RequestBody WorkOrderRequest request) {

        WorkOrderResponse response =
                workOrderService.createWorkOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // DELETE WORK ORDER
    // MANAGER ONLY
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteWorkOrder(
            @PathVariable Long id) {

        try {
            workOrderService.deleteWorkOrder(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}