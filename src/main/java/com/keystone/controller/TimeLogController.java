package com.keystone.controller;

import com.keystone.model.TimeLog;
import com.keystone.service.TimeLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders/{workOrderId}/time-logs")
public class TimeLogController {

    private final TimeLogService timeLogService;

    public TimeLogController(
            TimeLogService timeLogService) {

        this.timeLogService = timeLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<TimeLog> addTimeLog(
            @PathVariable Long workOrderId,
            @RequestParam Long technicianId,
            @RequestParam Integer minutes,
            @RequestParam(required = false) String note) {

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
    public ResponseEntity<List<TimeLog>> getTimeLogs(
            @PathVariable Long workOrderId) {

        return ResponseEntity.ok(
                timeLogService.getTimeLogsForWorkOrder(
                        workOrderId
                )
        );
    }

    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'TECHNICIAN')")
    public ResponseEntity<Integer> getTotalMinutes(
            @PathVariable Long workOrderId) {

        return ResponseEntity.ok(
                timeLogService.getTotalMinutes(
                        workOrderId
                )
        );
    }
}