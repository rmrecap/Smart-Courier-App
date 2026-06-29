package com.smartcourier.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartcourier.core.domain.model.SYNC_CLEAN

@Entity(
    tableName = "users",
    indices = [Index(value = ["syncStatus"])]
)
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String = "",
    val email: String = "",
    val subscriptionTier: String = "FREE",
    val totalEarningsAed: Double = 0.0,
    val syncStatus: Int = SYNC_CLEAN,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val countryCode: String = "ae",
    val versionClock: Long = 0
)
