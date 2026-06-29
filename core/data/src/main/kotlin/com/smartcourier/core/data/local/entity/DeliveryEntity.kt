package com.smartcourier.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartcourier.core.domain.model.SYNC_CLEAN

@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["routeId", "status"])
    ]
)
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val routeId: String = "",
    val index: Int = 0,
    val recipientName: String = "",
    val recipientPhone: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "PENDING",
    val trackingToken: String = "",
    val earningsAed: Double = 0.0,
    val tipAmountAed: Double = 0.0,
    val localPhotoPath: String? = null,
    val photoRemoteUrl: String? = null,
    val syncStatus: Int = SYNC_CLEAN,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val countryCode: String = "ae",
    val versionClock: Long = 0
)
