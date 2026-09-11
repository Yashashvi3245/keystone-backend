package com.keystone.controller;

import com.keystone.dto.WorkOrderResponse;
import com.keystone.service.SlaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class SlaController {

    private final SlaService slaService;

    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    /**
     * Returns overdue work orders as DTOs (not raw entities).
     * Only MANAGER and DISPATCHER can access this endpoint.
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<WorkOrderResponse>> getOverdueWorkOrders() {
        return ResponseEntity.ok(slaService.getOverdueWorkOrders());
    }
}
