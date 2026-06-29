package com.smartcourier.core.domain.model

data class Delivery(
    val id: String,
    val routeId: String,
    val index: Int,
    val recipientName: String = "",
    val recipientPhone: String = "",
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = DeliveryStatus.PENDING.value,
    val trackingToken: String = "",
    val earningsAed: Double = 0.0,
    val tipAmountAed: Double = 0.0,
    val countryCode: String = "ae",
    val versionClock: Long = 0,
    val lastModifiedTimestamp: Long = System.currentTimeMillis()
)

enum class DeliveryStatus(val value: String) {
    PENDING("PENDING"),
    IN_TRANSIT("IN_TRANSIT"),
    DELIVERED("DELIVERED"),
    FAILED("FAILED")
}
