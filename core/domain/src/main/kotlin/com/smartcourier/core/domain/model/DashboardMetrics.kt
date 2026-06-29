package com.smartcourier.core.domain.model

data class DashboardMetrics(
    val activeRoutes: Int = 0,
    val totalDeliveriesToday: Int = 0,
    val completedToday: Int = 0,
    val totalEarningsToday: Double = 0.0
)
