package com.keystone.dto;

import java.util.Map;

public record DashboardResponse(

        long totalWorkOrders,

        long overdueWorkOrders,

        Map<String, Long> statusCounts,

        Map<String, Long> priorityCounts

) {
}