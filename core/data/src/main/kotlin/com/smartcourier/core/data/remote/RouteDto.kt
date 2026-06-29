package com.smartcourier.core.data.remote

data class RouteDto(
    val routeId: String = "",
    val userId: String = "",
    val routeStatus: String = "CREATED",
    val totalDistanceMeters: Double = 0.0,
    val estimatedDurationSeconds: Long = 0,
    val lastModifiedTimestamp: Long = 0L,
    val versionClock: Long = 0,
    val countryCode: String = "ae"
)
