package com.keystone.service;

import com.keystone.model.TimeLog;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.repository.TimeLogRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public TimeLogService(
            TimeLogRepository timeLogRepository,
            WorkOrderRepository workOrderRepository,
            UserRepository userRepository) {

        this.timeLogRepository = timeLogRepository;
        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TimeLog addTimeLog(
            Long workOrderId,
            Long technicianId,
            Integer minutes,
            String note) {

        if (minutes == null || minutes <= 0) {

            throw new IllegalArgumentException(
                    "Minutes must be greater than zero"
            );
        }

        WorkOrder workOrder =
                workOrderRepository.findById(workOrderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        User technician =
                userRepository.findById(technicianId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Technician not found"
                                ));

        if (technician.getRole() == null
                || !"TECHNICIAN".equalsIgnoreCase(
                technician.getRole().name())) {

            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        if (workOrder.getAssignee() == null) {

            throw new IllegalStateException(
                    "Work order is not assigned to a technician"
            );
        }

        if (!workOrder.getAssignee()
                .getId()
                .equals(technician.getId())) {

            throw new IllegalStateException(
                    "Technician can log time only for assigned work orders"
            );
        }

        TimeLog timeLog =
                new TimeLog();

        timeLog.setWorkOrder(workOrder);
        timeLog.setTechnician(technician);
        timeLog.setMinutes(minutes);
        timeLog.setNote(note);
        timeLog.setLoggedAt(
                LocalDateTime.now()
        );

        return timeLogRepository.save(
                timeLog
        );
    }

    public List<TimeLog> getTimeLogsForWorkOrder(
            Long workOrderId) {

        if (!workOrderRepository.existsById(workOrderId)) {

            throw new RuntimeException(
                    "Work order not found"
            );
        }

        return timeLogRepository
                .findByWorkOrderIdOrderByLoggedAtAsc(
                        workOrderId
                );
    }

    public Integer getTotalMinutes(
            Long workOrderId) {

        return timeLogRepository
                .findByWorkOrderIdOrderByLoggedAtAsc(
                        workOrderId
                )
                .stream()
                .mapToInt(TimeLog::getMinutes)
                .sum();
    }
}