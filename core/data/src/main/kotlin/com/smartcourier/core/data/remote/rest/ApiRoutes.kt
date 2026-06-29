package com.smartcourier.core.data.remote.rest

data class ApiConfig(
    val baseUrl: String = "https://api.smartcourier.app",
    val apiVersion: String = "v1"
)

object ApiRoutes {
    private var config = ApiConfig()

    fun configure(baseUrl: String, apiVersion: String = "v1") {
        config = ApiConfig(baseUrl, apiVersion)
    }

    private val base get() = "${config.baseUrl}/api/${config.apiVersion}"

    val deliveries get() = "$base/deliveries"
    fun delivery(id: String) = "$base/deliveries/$id"
    fun completeDelivery(id: String) = "$base/deliveries/$id/complete"
    fun failDelivery(id: String) = "$base/deliveries/$id/fail"
    fun tipDelivery(id: String) = "$base/deliveries/$id/tip"
    val todaySummary get() = "$base/deliveries/summary/today"
    val historyLedger get() = "$base/deliveries/history"
    val weeklyEarnings get() = "$base/deliveries/earnings/weekly"
}
