package com.keystone.service;

import com.keystone.dto.DashboardResponse;
import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderStatus;
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

    public DashboardResponse getDashboard(
            String status,
            String priority,
            String technician,
            String site) {

        List<WorkOrder> workOrders =
                workOrderRepository.findAll();

        List<WorkOrder> filteredWorkOrders =
                workOrders.stream()
                        .filter(workOrder ->
                                matchesStatus(
                                        workOrder,
                                        status
                                ))
                        .filter(workOrder ->
                                matchesPriority(
                                        workOrder,
                                        priority
                                ))
                        .filter(workOrder ->
                                matchesTechnician(
                                        workOrder,
                                        technician
                                ))
                        .filter(workOrder ->
                                matchesSite(
                                        workOrder,
                                        site
                                ))
                        .toList();

        long totalWorkOrders =
                filteredWorkOrders.size();

        long overdueWorkOrders =
                filteredWorkOrders.stream()
                        .filter(this::isOverdue)
                        .count();

        long completedWorkOrders =
                filteredWorkOrders.stream()
                        .filter(workOrder ->
                                workOrder.getStatus()
                                        == WorkOrderStatus.COMPLETED
                        )
                        .count();

        long inProgressWorkOrders =
                filteredWorkOrders.stream()
                        .filter(workOrder ->
                                workOrder.getStatus()
                                        == WorkOrderStatus.IN_PROGRESS
                        )
                        .count();

        long slaApplicableWorkOrders =
                filteredWorkOrders.stream()
                        .filter(workOrder ->
                                workOrder.getSlaDueDate() != null
                                        && workOrder.getStatus() != null
                                        && workOrder.getStatus()
                                        != WorkOrderStatus.CANCELLED
                        )
                        .count();

        long slaCompliantWorkOrders =
                filteredWorkOrders.stream()
                        .filter(this::isSlaCompliant)
                        .count();

        double slaCompliancePercentage =
                slaApplicableWorkOrders == 0
                        ? 100.0
                        : (slaCompliantWorkOrders * 100.0)
                        / slaApplicableWorkOrders;

        Map<String, Long> statusCounts =
                new LinkedHashMap<>();

        filteredWorkOrders.stream()
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

        filteredWorkOrders.stream()
                .filter(workOrder ->
                        workOrder.getPriority() != null)
                .forEach(workOrder ->
                        priorityCounts.merge(
                                workOrder.getPriority().name(),
                                1L,
                                Long::sum
                        )
                );

        Map<String, Long> technicianCounts =
                new LinkedHashMap<>();

        filteredWorkOrders.stream()
                .filter(workOrder ->
                        workOrder.getAssignee() != null)
                .forEach(workOrder ->
                        technicianCounts.merge(
                                workOrder.getAssignee().getEmail(),
                                1L,
                                Long::sum
                        )
                );

        Map<String, Long> siteCounts =
                new LinkedHashMap<>();

        filteredWorkOrders.stream()
                .filter(workOrder ->
                        workOrder.getSite() != null)
                .forEach(workOrder ->
                        siteCounts.merge(
                                workOrder.getSite().getName(),
                                1L,
                                Long::sum
                        )
                );

        return new DashboardResponse(
                totalWorkOrders,
                overdueWorkOrders,
                completedWorkOrders,
                inProgressWorkOrders,
                Math.round(
                        slaCompliancePercentage * 100.0
                ) / 100.0,
                statusCounts,
                priorityCounts,
                technicianCounts,
                siteCounts
        );
    }

    private boolean matchesStatus(
            WorkOrder workOrder,
            String status) {

        if (status == null || status.isBlank()) {
            return true;
        }

        if (workOrder.getStatus() == null) {
            return false;
        }

        return workOrder.getStatus()
                .name()
                .equalsIgnoreCase(status);
    }

    private boolean matchesPriority(
            WorkOrder workOrder,
            String priority) {

        if (priority == null || priority.isBlank()) {
            return true;
        }

        if (workOrder.getPriority() == null) {
            return false;
        }

        return workOrder.getPriority()
                .name()
                .equalsIgnoreCase(priority);
    }

    private boolean matchesTechnician(
            WorkOrder workOrder,
            String technician) {

        if (technician == null || technician.isBlank()) {
            return true;
        }

        if (workOrder.getAssignee() == null) {
            return false;
        }

        return workOrder.getAssignee()
                .getEmail()
                .equalsIgnoreCase(technician);
    }

    private boolean matchesSite(
            WorkOrder workOrder,
            String site) {

        if (site == null || site.isBlank()) {
            return true;
        }

        if (workOrder.getSite() == null) {
            return false;
        }

        return workOrder.getSite()
                .getName()
                .equalsIgnoreCase(site);
    }

    private boolean isOverdue(
            WorkOrder workOrder) {

        if (workOrder.getSlaDueDate() == null) {
            return false;
        }

        if (workOrder.getStatus() == null) {
            return false;
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.COMPLETED
                || workOrder.getStatus()
                == WorkOrderStatus.CLOSED
                || workOrder.getStatus()
                == WorkOrderStatus.CANCELLED) {

            return false;
        }

        return workOrder.getSlaDueDate()
                .isBefore(LocalDateTime.now());
    }

    private boolean isSlaCompliant(
            WorkOrder workOrder) {

        if (workOrder.getSlaDueDate() == null) {
            return false;
        }

        if (workOrder.getStatus() == null) {
            return false;
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.CANCELLED) {

            return false;
        }

        return !workOrder.getSlaDueDate()
                .isBefore(LocalDateTime.now());
    }
}