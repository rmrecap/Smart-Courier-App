package com.smartcourier.core.domain.model

data class DailyEarnings(
    val dayOfWeek: Int,
    val label: String,
    val earnings: Double,
    val tipAmount: Double = 0.0
)
