package com.keystone.dto;

import com.keystone.model.WorkOrderStatus;

import java.time.LocalDateTime;

public record WorkOrderHistoryResponse(
        Long id,
        Long workOrderId,
        WorkOrderStatus fromStatus,
        WorkOrderStatus toStatus,
        Long changedById,
        String changedByEmail,
        LocalDateTime changedAt,
        String note
) {
}