package com.keystone.controller;

import com.keystone.model.Part;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderPart;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.service.WorkOrderPartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkOrderPartController {

    private final WorkOrderPartService workOrderPartService;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;

    public WorkOrderPartController(
            WorkOrderPartService workOrderPartService,
            UserRepository userRepository,
            WorkOrderRepository workOrderRepository) {

        this.workOrderPartService = workOrderPartService;
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @PostMapping("/work-orders/{workOrderId}/parts")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> addPart(
            @PathVariable Long workOrderId,
            @RequestParam Long partId,
            @RequestParam Integer quantity,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        User currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (currentUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        /*
         * Technician security:
         * A technician can only use parts on their own assigned work order.
         */
        if (currentUser.getRole().name().equals("TECHNICIAN")) {

            WorkOrder workOrder = workOrderRepository
                    .findById(workOrderId)
                    .orElse(null);

            if (workOrder == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Work order not found");
            }

            if (workOrder.getAssignee() == null) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You can only add parts to work orders assigned to you");
            }

            if (!workOrder.getAssignee().getId().equals(currentUser.getId())) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You can only add parts to work orders assigned to you");
            }
        }

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
    public ResponseEntity<?> getParts(
            @PathVariable Long workOrderId,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        User currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (currentUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        /*
         * Technician can only view parts used on their assigned work orders.
         */
        if (currentUser.getRole().name().equals("TECHNICIAN")) {

            WorkOrder workOrder = workOrderRepository
                    .findById(workOrderId)
                    .orElse(null);

            if (workOrder == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Work order not found");
            }

            if (workOrder.getAssignee() == null
                    || !workOrder.getAssignee().getId().equals(currentUser.getId())) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You can only view parts for work orders assigned to you");
            }
        }

        return ResponseEntity.ok(
                workOrderPartService.getPartsForWorkOrder(workOrderId)
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