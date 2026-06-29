package com.smartcourier.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartcourier.core.domain.model.SYNC_CLEAN

@Entity(
    tableName = "routes",
    indices = [
        Index(value = ["syncStatus"]),
        Index(value = ["userId"])
    ]
)
data class RouteEntity(
    @PrimaryKey val routeId: String,
    val userId: String = "",
    val routeStatus: String = "CREATED",
    val totalDistanceMeters: Double = 0.0,
    val estimatedDurationSeconds: Long = 0,
    val syncStatus: Int = SYNC_CLEAN,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val countryCode: String = "ae",
    val versionClock: Long = 0
)
