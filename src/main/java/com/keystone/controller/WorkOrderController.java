package com.keystone.controller;

import com.keystone.dto.WorkOrderResponse;
import com.keystone.dto.WorkOrderRequest;
import com.keystone.service.WorkOrderService;
import com.keystone.model.WorkOrderStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    // GET ALL / SEARCH /
    // FILTER / PAGINATION
    // =========================
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> getWorkOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {

        // Prevent invalid pagination values
        if (page < 0) {
            page = 0;
        }

        if (size < 1) {
            size = 10;
        }

        // Prevent extremely large page requests
        if (size > 100) {
            size = 100;
        }

        // Allowed sorting fields
        String safeSortBy = switch (sortBy) {
            case "id", "code", "title", "priority",
                 "status", "slaDueDate" -> sortBy;
            default -> "id";
        };

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(sortDirection, safeSortBy)
                );

        Page<WorkOrderResponse> result =
                workOrderService.searchWorkOrders(
                        search,
                        status,
                        priority,
                        pageable
                );

        // =========================
        // MANAGER / DISPATCHER
        // =========================
        if (hasRole(authentication, "MANAGER")
                || hasRole(authentication, "DISPATCHER")) {

            return ResponseEntity.ok(result);
        }

        // =========================
        // TECHNICIAN
        // =========================
        // Technician must only receive
        // work orders assigned to them.
        List<WorkOrderResponse> assignedWorkOrders =
                result.getContent()
                        .stream()
                        .filter(workOrder ->
                                isAssignedToCurrentUser(
                                        workOrder,
                                        authentication
                                ))
                        .toList();

        return ResponseEntity.ok(
                new TechnicianPageResponse(
                        assignedWorkOrders,
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isFirst(),
                        result.isLast()
                )
        );
    }

    // =========================
    // GET BY ID
    // =========================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> getWorkOrderById(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            WorkOrderResponse workOrder =
                    workOrderService.getWorkOrderById(id);

            if (hasRole(authentication, "MANAGER")
                    || hasRole(authentication, "DISPATCHER")) {

                return ResponseEntity.ok(workOrder);
            }

            if (isAssignedToCurrentUser(
                    workOrder,
                    authentication)) {

                return ResponseEntity.ok(workOrder);
            }

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this work order"
                    );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Work order not found");
        }
    }

    // =========================
    // GET BY CODE
    // =========================
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> getWorkOrderByCode(
            @PathVariable String code,
            Authentication authentication) {

        try {

            WorkOrderResponse workOrder =
                    workOrderService.getWorkOrderByCode(code);

            if (hasRole(authentication, "MANAGER")
                    || hasRole(authentication, "DISPATCHER")) {

                return ResponseEntity.ok(workOrder);
            }

            if (isAssignedToCurrentUser(
                    workOrder,
                    authentication)) {

                return ResponseEntity.ok(workOrder);
            }

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this work order"
                    );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Work order not found");
        }
    }

    // =========================
    // GET WORK ORDER HISTORY
    // =========================
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> getWorkOrderHistory(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            WorkOrderResponse workOrder =
                    workOrderService.getWorkOrderById(id);

            if (hasRole(authentication, "MANAGER")
                    || hasRole(authentication, "DISPATCHER")) {

                return ResponseEntity.ok(
                        workOrderService.getWorkOrderHistory(id)
                );
            }

            if (isAssignedToCurrentUser(
                    workOrder,
                    authentication)) {

                return ResponseEntity.ok(
                        workOrderService.getWorkOrderHistory(id)
                );
            }

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this work order history"
                    );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Work order not found");
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
    public ResponseEntity<?> updateWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderRequest request) {

        try {

            return ResponseEntity.ok(
                    workOrderService.updateWorkOrder(
                            id,
                            request
                    )
            );

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // =========================
    // UPDATE STATUS
    // =========================
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam WorkOrderStatus status,
            Authentication authentication) {

        try {

            WorkOrderResponse workOrder =
                    workOrderService.getWorkOrderById(id);

            // =========================
            // TECHNICIAN SECURITY
            // =========================
            if (hasRole(authentication, "TECHNICIAN")) {

                if (!isAssignedToCurrentUser(
                        workOrder,
                        authentication)) {

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(
                                    "Technician can update only assigned work orders"
                            );
                }

                if (status != WorkOrderStatus.IN_PROGRESS
                        && status != WorkOrderStatus.ON_HOLD
                        && status != WorkOrderStatus.COMPLETED) {

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(
                                    "Technician is not allowed to change work order to "
                                            + status
                            );
                }
            }

            return ResponseEntity.ok(
                    workOrderService.updateStatus(
                            id,
                            status
                    )
            );

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Work order not found");
        }
    }

    // =========================
    // ASSIGN WORK ORDER
    // =========================
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> assignWorkOrder(
            @PathVariable Long id,
            @RequestParam Long assigneeId) {

        try {

            return ResponseEntity.ok(
                    workOrderService.assignWorkOrder(
                            id,
                            assigneeId
                    )
            );

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteWorkOrder(
            @PathVariable Long id) {

        try {

            workOrderService.deleteWorkOrder(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Work order not found");
        }
    }

    // =========================
    // CHECK USER ROLE
    // =========================
    private boolean hasRole(
            Authentication authentication,
            String role) {

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_" + role)
                );
    }

    // =========================
    // CHECK WORK ORDER ASSIGNEE
    // =========================
    private boolean isAssignedToCurrentUser(
            WorkOrderResponse workOrder,
            Authentication authentication) {

        if (authentication == null
                || workOrder == null
                || workOrder.assigneeEmail() == null) {

            return false;
        }

        return workOrder.assigneeEmail()
                .equalsIgnoreCase(
                        authentication.getName()
                );
    }

    // =========================
    // TECHNICIAN PAGINATION
    // RESPONSE
    // =========================
    private record TechnicianPageResponse(
            List<WorkOrderResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }
}