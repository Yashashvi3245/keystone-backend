package com.keystone.dto;

import com.keystone.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record WorkOrderRequest(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Priority is required")
        Priority priority,

        @NotNull(message = "SLA due date is required")
        LocalDateTime slaDueDate,

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Site ID is required")
        Long siteId,

        Long assigneeId
) {
}