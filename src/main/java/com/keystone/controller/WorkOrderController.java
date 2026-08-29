package com.keystone.controller;

import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.WorkOrderStatus;
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
    // GET ALL
    // =========================
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public List<WorkOrderResponse> getAllWorkOrders() {
        return workOrderService.getAllWorkOrders();
    }

    // =========================
    // GET BY ID
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
    // GET BY CODE
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
    // CREATE
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
    // UPDATE
    // =========================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<WorkOrderResponse> updateWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderRequest request) {

        try {
            return ResponseEntity.ok(
                    workOrderService.updateWorkOrder(id, request)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================
    // UPDATE STATUS
    // =========================
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam WorkOrderStatus status) {

        try {
            return ResponseEntity.ok(
                    workOrderService.updateStatus(id, status)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================
    // ASSIGN WORK ORDER
    // =========================
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<WorkOrderResponse> assignWorkOrder(
            @PathVariable Long id,
            @RequestParam Long assigneeId) {

        try {
            return ResponseEntity.ok(
                    workOrderService.assignWorkOrder(
                            id,
                            assigneeId
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================
    // DELETE
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