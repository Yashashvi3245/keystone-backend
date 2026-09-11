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

    // -------------------------------------------------------
    // ALL-USERS: search / filter / pagination
    // -------------------------------------------------------

    Page<WorkOrder> findByTitleContainingIgnoreCase(
            String title, Pageable pageable);

    Page<WorkOrder> findByStatus(
            WorkOrderStatus status, Pageable pageable);

    Page<WorkOrder> findByPriority(
            String priority, Pageable pageable);

    Page<WorkOrder> findByTitleContainingIgnoreCaseAndStatus(
            String title, WorkOrderStatus status, Pageable pageable);

    Page<WorkOrder> findByTitleContainingIgnoreCaseAndPriority(
            String title, String priority, Pageable pageable);

    Page<WorkOrder> findByStatusAndPriority(
            WorkOrderStatus status, String priority, Pageable pageable);

    Page<WorkOrder> findByTitleContainingIgnoreCaseAndStatusAndPriority(
            String title, WorkOrderStatus status, String priority, Pageable pageable);

    // -------------------------------------------------------
    // CUSTOMER portal: scoped to customer_id
    // -------------------------------------------------------

    Page<WorkOrder> findByCustomer_Id(
            Long customerId, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndTitleContainingIgnoreCase(
            Long customerId, String title, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndStatus(
            Long customerId, WorkOrderStatus status, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndPriority(
            Long customerId, String priority, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndTitleContainingIgnoreCaseAndStatus(
            Long customerId, String title, WorkOrderStatus status, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndTitleContainingIgnoreCaseAndPriority(
            Long customerId, String title, String priority, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndStatusAndPriority(
            Long customerId, WorkOrderStatus status, String priority, Pageable pageable);

    Page<WorkOrder> findByCustomer_IdAndTitleContainingIgnoreCaseAndStatusAndPriority(
            Long customerId, String title, WorkOrderStatus status, String priority, Pageable pageable);

    // -------------------------------------------------------
    // TECHNICIAN: scoped to assignee_id  (fixes pagination bug)
    // Without these queries the controller was loading ALL work orders
    // and filtering in-memory, producing wrong pagination metadata.
    // -------------------------------------------------------

    Page<WorkOrder> findByAssignee_Id(
            Long assigneeId, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndTitleContainingIgnoreCase(
            Long assigneeId, String title, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndStatus(
            Long assigneeId, WorkOrderStatus status, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndPriority(
            Long assigneeId, String priority, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndTitleContainingIgnoreCaseAndStatus(
            Long assigneeId, String title, WorkOrderStatus status, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndTitleContainingIgnoreCaseAndPriority(
            Long assigneeId, String title, String priority, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndStatusAndPriority(
            Long assigneeId, WorkOrderStatus status, String priority, Pageable pageable);

    Page<WorkOrder> findByAssignee_IdAndTitleContainingIgnoreCaseAndStatusAndPriority(
            Long assigneeId, String title, WorkOrderStatus status, String priority, Pageable pageable);
}
