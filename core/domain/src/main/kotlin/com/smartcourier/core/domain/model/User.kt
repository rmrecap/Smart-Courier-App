package com.smartcourier.core.domain.model

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val subscriptionTier: UserTier = UserTier.FREE,
    val totalEarningsAed: Double = 0.0,
    val syncStatus: Int = SYNC_CLEAN,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val countryCode: String = "ae",
    val versionClock: Long = 0
)

enum class UserTier {
    FREE, PRO, BUSINESS
}
