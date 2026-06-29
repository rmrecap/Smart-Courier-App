package com.smartcourier.core.domain.repository

import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.TodaySummary
import kotlinx.coroutines.flow.Flow

interface DeliveryRepository {
    suspend fun upsertDeliveries(deliveries: List<Delivery>)
    suspend fun getDelivery(deliveryId: String): Delivery?
    fun observeDeliveriesForRoute(routeId: String): Flow<List<Delivery>>
    suspend fun markDeliveryComplete(deliveryId: String, localPhotoPath: String, earnings: Double)
    suspend fun markDeliveryFailed(deliveryId: String)
    suspend fun fetchUnsyncedDeliveries(): List<Delivery>
    suspend fun getTodaySummary(countryCode: String = "ae"): TodaySummary
    fun observeHistoryLedger(countryCode: String = "ae"): Flow<List<Delivery>>
    fun observeWeeklyEarnings(countryCode: String = "ae"): Flow<List<com.smartcourier.core.domain.model.DailyEarnings>>
    suspend fun logTip(deliveryId: String, amount: Double)
}
