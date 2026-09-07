package com.keystone.controller;

import com.keystone.model.Part;
import com.keystone.model.WorkOrderPart;
import com.keystone.service.WorkOrderPartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkOrderPartController {

    private final WorkOrderPartService workOrderPartService;

    public WorkOrderPartController(
            WorkOrderPartService workOrderPartService) {

        this.workOrderPartService = workOrderPartService;
    }

    @PostMapping("/work-orders/{workOrderId}/parts")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<WorkOrderPart> addPart(
            @PathVariable Long workOrderId,
            @RequestParam Long partId,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                workOrderPartService.addPart(
                        workOrderId,
                        partId,
                        quantity
                )
        );
    }

    @GetMapping("/work-orders/{workOrderId}/parts")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<List<WorkOrderPart>> getParts(
            @PathVariable Long workOrderId) {

        return ResponseEntity.ok(
                workOrderPartService
                        .getPartsForWorkOrder(workOrderId)
        );
    }

    @PostMapping("/parts")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Part> createPart(
            @RequestParam String name,
            @RequestParam Integer stockQuantity,
            @RequestParam Integer unitPrice) {

        return ResponseEntity.ok(
                workOrderPartService.createPart(
                        name,
                        stockQuantity,
                        unitPrice
                )
        );
    }

    @GetMapping("/parts")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<List<Part>> getAllParts() {

        return ResponseEntity.ok(
                workOrderPartService.getAllParts()
        );
    }
}