package com.smartcourier.core.domain.model

data class TodaySummary(
    val totalDeliveries: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val totalEarnings: Double = 0.0,
    val totalTips: Double = 0.0
)
