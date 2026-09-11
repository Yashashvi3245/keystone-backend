package com.keystone.dto;

import java.util.Map;

public record DashboardResponse(

        long totalWorkOrders,

        long overdueWorkOrders,

        long completedWorkOrders,

        long inProgressWorkOrders,

        double slaCompliancePercentage,

        Map<String, Long> statusCounts,

        Map<String, Long> priorityCounts,

        Map<String, Long> technicianCounts,

        Map<String, Long> siteCounts

) {
}