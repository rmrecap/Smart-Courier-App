package com.smartcourier.core.data.repository

import android.content.Context
import com.smartcourier.core.data.local.RoomTransactionRunner
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.entity.OutboxEntity
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.data.mapper.toEntity
import com.smartcourier.core.data.sync.SyncScheduler
import com.smartcourier.core.domain.model.DailyEarnings
import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.TodaySummary
import com.smartcourier.core.domain.repository.DeliveryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
class OfflineFirstDeliveryRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val deliveryDao: DeliveryDao,
    private val outboxDao: OutboxDao,
    private val transactionRunner: RoomTransactionRunner
) : DeliveryRepository {

    override suspend fun upsertDeliveries(deliveries: List<Delivery>) {
        deliveryDao.upsertDeliveries(deliveries.map { d ->
            val existing = deliveryDao.getDelivery(d.id)
            d.toEntity().copy(versionClock = (existing?.versionClock ?: 0) + 1)
        })
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
            if (outboxDao.countPending(deliveryId, "DELIVERY_COMPLETE") == 0) {
                outboxDao.enqueue(
                    OutboxEntity(
                        targetId = deliveryId,
                        payloadType = "DELIVERY_COMPLETE",
                        serializedPayload = earnings.toString(),
                        clientEventId = UUID.randomUUID().toString()
                    )
                )
            }
        }
        SyncScheduler.scheduleImmediate(applicationContext)
    }

    override suspend fun markDeliveryFailed(deliveryId: String) {
        transactionRunner.runInTransaction {
            deliveryDao.markComplete(deliveryId, "FAILED", null, 0.0)
            if (outboxDao.countPending(deliveryId, "DELIVERY_FAILED") == 0) {
                outboxDao.enqueue(
                    OutboxEntity(
                        targetId = deliveryId,
                        payloadType = "DELIVERY_FAILED",
                        serializedPayload = "0",
                        clientEventId = UUID.randomUUID().toString()
                    )
                )
            }
        }
        SyncScheduler.scheduleImmediate(applicationContext)
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
    }
}
