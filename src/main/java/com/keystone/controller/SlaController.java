package com.keystone.controller;

import com.keystone.model.WorkOrder;
import com.keystone.service.SlaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class SlaController {

    private final SlaService slaService;

    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<WorkOrder>> getOverdueWorkOrders() {

        return ResponseEntity.ok(
                slaService.getOverdueWorkOrders()
        );
    }
}