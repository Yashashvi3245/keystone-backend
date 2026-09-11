package com.keystone.controller;

import com.keystone.model.TimeLog;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.service.TimeLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders/{workOrderId}/time-logs")
public class TimeLogController {

    private final TimeLogService timeLogService;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;

    public TimeLogController(
            TimeLogService timeLogService,
            UserRepository userRepository,
            WorkOrderRepository workOrderRepository) {

        this.timeLogService = timeLogService;
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> addTimeLog(
            @PathVariable Long workOrderId,
            @RequestParam Long technicianId,
            @RequestParam Integer minutes,
            @RequestParam(required = false) String note,
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
         * A technician can only log time against their own assigned work order.
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
                        .body("You can only log time on work orders assigned to you");
            }

            if (!workOrder.getAssignee().getId().equals(currentUser.getId())) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You can only log time on work orders assigned to you");
            }

            /*
             * Prevent technicianId spoofing.
             * The technicianId must match the authenticated technician.
             */
            if (!technicianId.equals(currentUser.getId())) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You can only log time for yourself");
            }
        }

        return ResponseEntity.ok(
                timeLogService.addTimeLog(
                        workOrderId,
                        technicianId,
                        minutes,
                        note
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> getTimeLogs(
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
         * Technician can only view time logs of their assigned work orders.
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
                        .body("You can only view time logs for work orders assigned to you");
            }
        }

        return ResponseEntity.ok(
                timeLogService.getTimeLogsForWorkOrder(workOrderId)
        );
    }

    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<?> getTotalMinutes(
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
         * Technician can only view total time of their assigned work orders.
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
                        .body("You can only view time totals for work orders assigned to you");
            }
        }

        return ResponseEntity.ok(
                timeLogService.getTotalMinutes(workOrderId)
        );
    }
}