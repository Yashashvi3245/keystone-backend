package com.keystone.service;

import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderStatus;
import com.keystone.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SlaService {

    private static final Logger logger =
            LoggerFactory.getLogger(SlaService.class);

    private final WorkOrderRepository workOrderRepository;
    private final NotificationService notificationService;

    public SlaService(
            WorkOrderRepository workOrderRepository,
            NotificationService notificationService) {

        this.workOrderRepository = workOrderRepository;
        this.notificationService = notificationService;
    }

    public LocalDateTime calculateDueDate(
            String priority) {

        if (priority == null || priority.isBlank()) {

            throw new IllegalArgumentException(
                    "Priority is required for SLA calculation"
            );
        }

        return switch (priority.toUpperCase()) {

            case "CRITICAL" ->
                    LocalDateTime.now().plusHours(4);

            case "HIGH" ->
                    LocalDateTime.now().plusHours(8);

            case "MEDIUM" ->
                    LocalDateTime.now().plusHours(24);

            case "LOW" ->
                    LocalDateTime.now().plusHours(48);

            default ->
                    throw new IllegalArgumentException(
                            "Invalid priority: " + priority
                    );
        };
    }

    @Transactional
    public WorkOrder applySlaDueDate(
            Long workOrderId) {

        WorkOrder workOrder =
                workOrderRepository.findById(
                        workOrderId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Work order not found"
                        ));

        if (workOrder.getPriority() == null) {

            throw new IllegalArgumentException(
                    "Work order priority is required"
            );
        }

        LocalDateTime dueDate =
                calculateDueDate(
                        workOrder.getPriority().name()
                );

        workOrder.setSlaDueDate(dueDate);

        return workOrderRepository.save(
                workOrder
        );
    }

    public boolean isSlaBreached(
            WorkOrder workOrder) {

        if (workOrder == null) {
            return false;
        }

        if (workOrder.getSlaDueDate() == null) {
            return false;
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.CLOSED
                || workOrder.getStatus()
                == WorkOrderStatus.CANCELLED
                || workOrder.getStatus()
                == WorkOrderStatus.COMPLETED) {

            return false;
        }

        return LocalDateTime.now()
                .isAfter(
                        workOrder.getSlaDueDate()
                );
    }

    public List<WorkOrder> getOverdueWorkOrders() {

        LocalDateTime now =
                LocalDateTime.now();

        return workOrderRepository.findAll()
                .stream()
                .filter(workOrder ->
                        workOrder.getSlaDueDate() != null
                                && workOrder.getSlaDueDate()
                                .isBefore(now)
                                && workOrder.getStatus()
                                != WorkOrderStatus.CLOSED
                                && workOrder.getStatus()
                                != WorkOrderStatus.CANCELLED
                                && workOrder.getStatus()
                                != WorkOrderStatus.COMPLETED
                )
                .toList();
    }

    public long getHoursRemaining(
            WorkOrder workOrder) {

        if (workOrder == null
                || workOrder.getSlaDueDate() == null) {

            return 0;
        }

        return ChronoUnit.HOURS.between(
                LocalDateTime.now(),
                workOrder.getSlaDueDate()
        );
    }

    @Scheduled(fixedRate = 300000)
    public void checkSlaBreaches() {

        List<WorkOrder> overdueWorkOrders =
                getOverdueWorkOrders();

        if (overdueWorkOrders.isEmpty()) {

            logger.info(
                    "SLA breach check completed: no overdue work orders"
            );

            return;
        }

        logger.warn(
                "SLA breach check: {} overdue work order(s) found",
                overdueWorkOrders.size()
        );

        for (WorkOrder workOrder :
                overdueWorkOrders) {

            logger.warn(
                    "SLA BREACHED - Work Order ID: {}, Code: {}, Due Date: {}, Status: {}",
                    workOrder.getId(),
                    workOrder.getCode(),
                    workOrder.getSlaDueDate(),
                    workOrder.getStatus()
            );

            notificationService.notifyManagersOfSlaBreach(
                    workOrder
            );
        }
    }
}