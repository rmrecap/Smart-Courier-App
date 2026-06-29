package com.smartcourier.core.data.remote

data class DeliveryDto(
    val id: String = "",
    val routeId: String = "",
    val index: Int = 0,
    val recipientName: String = "",
    val recipientPhone: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "PENDING",
    val trackingToken: String = "",
    val photoRemoteUrl: String? = null,
    val earningsAed: Double = 0.0,
    val tipAmountAed: Double = 0.0,
    val lastModifiedTimestamp: Long = 0L,
    val versionClock: Long = 0,
    val countryCode: String = "ae",
    val expireAt: Long = 0
)
