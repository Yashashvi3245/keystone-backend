package com.keystone.service;

import com.keystone.dto.DashboardResponse;
import com.keystone.model.WorkOrder;
import com.keystone.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final WorkOrderRepository workOrderRepository;

    public DashboardService(
            WorkOrderRepository workOrderRepository) {

        this.workOrderRepository = workOrderRepository;
    }

    public DashboardResponse getDashboard() {

        List<WorkOrder> workOrders =
                workOrderRepository.findAll();

        long totalWorkOrders =
                workOrders.size();

        long overdueWorkOrders =
                workOrders.stream()
                        .filter(this::isOverdue)
                        .count();

        Map<String, Long> statusCounts =
                new LinkedHashMap<>();

        workOrders.stream()
                .filter(workOrder ->
                        workOrder.getStatus() != null)
                .forEach(workOrder ->
                        statusCounts.merge(
                                workOrder.getStatus().name(),
                                1L,
                                Long::sum
                        )
                );

        Map<String, Long> priorityCounts =
                new LinkedHashMap<>();

        workOrders.stream()
                .filter(workOrder ->
                        workOrder.getPriority() != null)
                .forEach(workOrder ->
                        priorityCounts.merge(
                                workOrder.getPriority().name(),
                                1L,
                                Long::sum
                        )
                );

        return new DashboardResponse(
                totalWorkOrders,
                overdueWorkOrders,
                statusCounts,
                priorityCounts
        );
    }

    private boolean isOverdue(
            WorkOrder workOrder) {

        if (workOrder.getSlaDueDate() == null) {
            return false;
        }

        if (workOrder.getStatus() == null) {
            return false;
        }

        String status =
                workOrder.getStatus().name();

        if ("COMPLETED".equals(status)
                || "CLOSED".equals(status)
                || "CANCELLED".equals(status)) {

            return false;
        }

        return workOrder.getSlaDueDate()
                .isBefore(LocalDateTime.now());
    }
}