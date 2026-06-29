package com.smartcourier.core.data.remote.rest

import android.content.Context
import com.google.gson.Gson
import com.smartcourier.core.data.local.RoomTransactionRunner
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.entity.OutboxEntity
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.data.mapper.toDto
import com.smartcourier.core.data.mapper.toEntity
import com.smartcourier.core.data.remote.DeliveryDto
import com.smartcourier.core.data.sync.SyncScheduler
import com.smartcourier.core.domain.model.DailyEarnings
import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.TodaySummary
import com.smartcourier.core.domain.repository.DeliveryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestDeliveryRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val deliveryDao: DeliveryDao,
    private val outboxDao: OutboxDao,
    private val transactionRunner: RoomTransactionRunner,
    private val httpClient: HttpClient
) : DeliveryRepository {

    private val gson = Gson()

    override suspend fun upsertDeliveries(deliveries: List<Delivery>) {
        val entities = deliveries.map { d ->
            val existing = deliveryDao.getDelivery(d.id)
            d.toEntity().copy(versionClock = (existing?.versionClock ?: 0) + 1)
        }
        deliveryDao.upsertDeliveries(entities)
        val dtos = entities.mapNotNull { entity ->
            val domain = entity.toDomain()
            domain.toDto()
        }
        runCatching { postDeliveryBatch(dtos) }
            .onFailure { scheduleBackgroundSync() }
    }

    override suspend fun getDelivery(deliveryId: String): Delivery? =
        deliveryDao.getDelivery(deliveryId)?.toDomain()

    override fun observeDeliveriesForRoute(routeId: String): Flow<List<Delivery>> =
        deliveryDao.observeByRoute(routeId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun fetchUnsyncedDeliveries(): List<Delivery> =
        deliveryDao.fetchUnsynced().map { it.toDomain() }

    override suspend fun markDeliveryComplete(deliveryId: String, localPhotoPath: String, earnings: Double) {
        transactionRunner.runInTransaction {
            deliveryDao.markComplete(deliveryId, "DELIVERED", localPhotoPath, earnings)
            outboxDao.enqueue(
                OutboxEntity(
                    targetId = deliveryId,
                    payloadType = "DELIVERY_COMPLETE",
                    serializedPayload = gson.toJson(mapOf("earnings" to earnings)),
                    clientEventId = UUID.randomUUID().toString()
                )
            )
        }
        val synced = runCatching { postCompleteDelivery(deliveryId, earnings) }.isSuccess
        if (synced) {
            outboxDao.removeByTarget(deliveryId, "DELIVERY_COMPLETE")
        } else {
            SyncScheduler.scheduleImmediate(applicationContext)
        }
    }

    override suspend fun markDeliveryFailed(deliveryId: String) {
        transactionRunner.runInTransaction {
            deliveryDao.markComplete(deliveryId, "FAILED", null, 0.0)
            outboxDao.enqueue(
                OutboxEntity(
                    targetId = deliveryId,
                    payloadType = "DELIVERY_FAILED",
                    serializedPayload = "{}",
                    clientEventId = UUID.randomUUID().toString()
                )
            )
        }
        val synced = runCatching { postFailDelivery(deliveryId) }.isSuccess
        if (synced) {
            outboxDao.removeByTarget(deliveryId, "DELIVERY_FAILED")
        } else {
            SyncScheduler.scheduleImmediate(applicationContext)
        }
    }

    override suspend fun getTodaySummary(countryCode: String): TodaySummary = withContext(Dispatchers.IO) {
        val since = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        TodaySummary(
            totalDeliveries = deliveryDao.countTotalSince(since, countryCode),
            completedCount = deliveryDao.countDeliveredSince(since, countryCode),
            failedCount = deliveryDao.countFailedSince(since, countryCode),
            totalEarnings = deliveryDao.sumEarningsSince(since, countryCode),
            totalTips = deliveryDao.sumTipsSince(since, countryCode)
        )
    }

    override fun observeHistoryLedger(countryCode: String): Flow<List<Delivery>> =
        deliveryDao.observeHistoryLedger(countryCode).map { entities -> entities.map { it.toDomain() } }

    override fun observeWeeklyEarnings(countryCode: String): Flow<List<DailyEarnings>> {
        val startOfWeek = LocalDate.now()
            .with(DayOfWeek.MONDAY)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return deliveryDao.observeCompletedSince(startOfWeek, countryCode).map { entities ->
            val zone = ZoneId.systemDefault()
            entities.groupBy { entity ->
                Instant.ofEpochMilli(entity.lastModifiedTimestamp)
                    .atZone(zone)
                    .toLocalDate()
                    .dayOfWeek.value
            }.map { (dayValue, deliveries) ->
                val dayOfWeek = DayOfWeek.of(dayValue)
                DailyEarnings(
                    dayOfWeek = dayValue,
                    label = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    earnings = deliveries.sumOf { it.earningsAed },
                    tipAmount = deliveries.sumOf { it.tipAmountAed }
                )
            }.sortedBy { it.dayOfWeek }
        }
    }

    override suspend fun logTip(deliveryId: String, amount: Double) {
        deliveryDao.logTip(deliveryId, amount)
        runCatching { postTipDelivery(deliveryId, amount) }
            .onFailure { scheduleBackgroundSync() }
    }

    private suspend fun postDeliveryBatch(dtos: List<DeliveryDto>) {
        val response: HttpResponse = httpClient.post(ApiRoutes.deliveries) {
            setBody(dtos)
        }
        require(response.status.isSuccess()) { "POST deliveries batch failed: ${response.status}" }
    }

    private suspend fun postCompleteDelivery(deliveryId: String, earnings: Double) {
        val response: HttpResponse = httpClient.post(ApiRoutes.completeDelivery(deliveryId)) {
            setBody(mapOf("earnings" to earnings))
        }
        require(response.status.isSuccess()) { "POST complete delivery failed: ${response.status}" }
    }

    private suspend fun postFailDelivery(deliveryId: String) {
        val response: HttpResponse = httpClient.post(ApiRoutes.failDelivery(deliveryId))
        require(response.status.isSuccess()) { "POST fail delivery failed: ${response.status}" }
    }

    private suspend fun postTipDelivery(deliveryId: String, amount: Double) {
        val response: HttpResponse = httpClient.post(ApiRoutes.tipDelivery(deliveryId)) {
            setBody(mapOf("amount" to amount))
        }
        require(response.status.isSuccess()) { "POST tip delivery failed: ${response.status}" }
    }

    private fun scheduleBackgroundSync() {
        SyncScheduler.scheduleImmediate(applicationContext)
    }
}
