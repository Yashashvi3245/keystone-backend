package com.keystone.dto;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long id,

        String message,

        boolean read,

        LocalDateTime createdAt

) {
}