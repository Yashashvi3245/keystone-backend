package com.keystone.dto;

import com.keystone.model.Priority;
import com.keystone.model.WorkOrderStatus;

import java.time.LocalDateTime;

public record WorkOrderResponse(

        Long id,

        String code,

        String title,

        String description,

        Priority priority,

        WorkOrderStatus status,

        LocalDateTime slaDueDate,

        Long customerId,

        String customerName,

        Long siteId,

        String siteName,

        Long assigneeId,

        String assigneeEmail

) {
}