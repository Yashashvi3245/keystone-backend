package com.keystone.repository;

import com.keystone.model.WorkOrderPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderPartRepository
        extends JpaRepository<WorkOrderPart, Long> {

    List<WorkOrderPart> findByWorkOrderId(Long workOrderId);
}