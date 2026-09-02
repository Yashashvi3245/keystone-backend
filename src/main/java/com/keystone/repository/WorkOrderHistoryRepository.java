package com.keystone.repository;

import com.keystone.model.WorkOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderHistoryRepository
        extends JpaRepository<WorkOrderHistory, Long> {

    List<WorkOrderHistory> findByWorkOrderIdOrderByChangedAtAsc(
            Long workOrderId
    );
}