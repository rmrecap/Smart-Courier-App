package com.smartcourier.core.data.remote

data class UserDto(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val subscriptionTier: String = "FREE",
    val totalEarningsAed: Double = 0.0,
    val lastModifiedTimestamp: Long = 0L,
    val versionClock: Long = 0,
    val countryCode: String = "ae"
)
