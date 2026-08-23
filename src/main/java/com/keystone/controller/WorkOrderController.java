package com.keystone.controller;

import com.keystone.model.WorkOrder;
import com.keystone.service.WorkOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public List<WorkOrder> getAllWorkOrders() {
        return workOrderService.getAllWorkOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getWorkOrderById(
            @PathVariable Long id) {

        return workOrderService.getWorkOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<WorkOrder> getWorkOrderByCode(
            @PathVariable String code) {

        return workOrderService.getWorkOrderByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public WorkOrder createWorkOrder(
            @RequestBody WorkOrder workOrder,
            @RequestParam Long customerId,
            @RequestParam Long siteId,
            @RequestParam(required = false) Long assigneeId) {

        return workOrderService.createWorkOrder(
                workOrder,
                customerId,
                siteId,
                assigneeId
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkOrder(
            @PathVariable Long id) {

        workOrderService.deleteWorkOrder(id);

        return ResponseEntity.noContent().build();
    }
}