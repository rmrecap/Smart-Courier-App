package com.smartcourier.app.dashboard

import com.smartcourier.core.domain.model.DailyEarnings
import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.TodaySummary
import com.smartcourier.core.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDeliveryRepository(
    initialSummary: TodaySummary = TodaySummary(),
    initialDeliveries: List<Delivery> = emptyList()
) : DeliveryRepository {

    private val _summary = MutableStateFlow(initialSummary)
    private val _deliveries = MutableStateFlow(initialDeliveries)
    private val _weeklyEarnings = MutableStateFlow(
        listOf(
            DailyEarnings(1, "Mon", 120.0, 15.0),
            DailyEarnings(2, "Tue", 80.0, 5.0),
            DailyEarnings(3, "Wed", 200.0, 25.0),
            DailyEarnings(4, "Thu", 150.0, 10.0),
            DailyEarnings(5, "Fri", 90.0, 0.0),
            DailyEarnings(6, "Sat", 50.0, 0.0),
            DailyEarnings(7, "Sun", 0.0, 0.0),
        )
    )

    private val _loggedTips = mutableMapOf<String, Double>()

    override suspend fun upsertDeliveries(deliveries: List<Delivery>) {}
    override suspend fun getDelivery(deliveryId: String): Delivery? = null
    override fun observeDeliveriesForRoute(routeId: String): Flow<List<Delivery>> =
        MutableStateFlow(emptyList()).asStateFlow()

    override suspend fun markDeliveryComplete(deliveryId: String, localPhotoPath: String, earnings: Double) {}
    override suspend fun markDeliveryFailed(deliveryId: String) {}
    override suspend fun fetchUnsyncedDeliveries(): List<Delivery> = emptyList()

    override suspend fun getTodaySummary(countryCode: String): TodaySummary = _summary.value

    override fun observeHistoryLedger(countryCode: String): Flow<List<Delivery>> =
        _deliveries.asStateFlow()

    override fun observeWeeklyEarnings(countryCode: String): Flow<List<DailyEarnings>> =
        _weeklyEarnings.asStateFlow()

    override suspend fun logTip(deliveryId: String, amount: Double) {
        _loggedTips[deliveryId] = amount
        val current = _summary.value
        _summary.value = current.copy(totalTips = current.totalTips + amount)
    }
}
