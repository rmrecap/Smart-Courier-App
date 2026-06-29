package com.smartcourier.core.domain.model

data class Route(
    val routeId: String,
    val userId: String,
    val routeStatus: RouteStatus = RouteStatus.CREATED,
    val totalDistanceMeters: Double = 0.0,
    val estimatedDurationSeconds: Long = 0,
    val syncStatus: Int = SYNC_CLEAN,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val deliveries: List<Delivery> = emptyList(),
    val countryCode: String = "ae",
    val versionClock: Long = 0
)

enum class RouteStatus(val value: String) {
    CREATED("CREATED"),
    OPTIMIZING("OPTIMIZING"),
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED")
}
