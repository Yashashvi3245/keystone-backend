package com.keystone.repository;

import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkOrderRepository
        extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByCode(String code);

    Page<WorkOrder> findByTitleContainingIgnoreCase(
            String title,
            Pageable pageable
    );

    Page<WorkOrder> findByStatus(
            WorkOrderStatus status,
            Pageable pageable
    );

    Page<WorkOrder> findByPriority(
            String priority,
            Pageable pageable
    );

    Page<WorkOrder> findByTitleContainingIgnoreCaseAndStatus(
            String title,
            WorkOrderStatus status,
            Pageable pageable
    );

    Page<WorkOrder> findByTitleContainingIgnoreCaseAndPriority(
            String title,
            String priority,
            Pageable pageable
    );

    Page<WorkOrder> findByStatusAndPriority(
            WorkOrderStatus status,
            String priority,
            Pageable pageable
    );

    Page<WorkOrder> findByTitleContainingIgnoreCaseAndStatusAndPriority(
            String title,
            WorkOrderStatus status,
            String priority,
            Pageable pageable
    );
}