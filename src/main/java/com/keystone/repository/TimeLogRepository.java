package com.keystone.repository;

import com.keystone.model.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeLogRepository
        extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findByWorkOrderIdOrderByLoggedAtAsc(
            Long workOrderId
    );
}