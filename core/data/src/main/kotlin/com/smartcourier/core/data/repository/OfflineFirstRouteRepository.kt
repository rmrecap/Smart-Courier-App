package com.smartcourier.core.data.repository

import android.content.Context
import com.smartcourier.core.data.local.RoomTransactionRunner
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.local.entity.OutboxEntity
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.data.mapper.toEntity
import com.smartcourier.core.data.sync.SyncScheduler
import com.smartcourier.core.domain.model.Coordinate
import com.smartcourier.core.domain.model.Route
import com.smartcourier.core.domain.model.RouteStatus
import com.smartcourier.core.domain.repository.RouteRepository
import com.smartcourier.core.domain.usecase.OptimizerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstRouteRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val routeDao: RouteDao,
    private val deliveryDao: DeliveryDao,
    private val outboxDao: OutboxDao,
    private val transactionRunner: RoomTransactionRunner,
    private val optimizerEngine: OptimizerEngine
) : RouteRepository {

    override suspend fun upsertRoute(route: Route) {
        val existing = routeDao.getRoute(route.routeId)
        val entity = route.toEntity().copy(versionClock = (existing?.versionClock ?: 0) + 1)
        transactionRunner.runInTransaction {
            routeDao.upsertRoute(entity)
            if (outboxDao.countPending(route.routeId, "ROUTE_UPSERT") == 0) {
                outboxDao.enqueue(
                    OutboxEntity(
                        targetId = route.routeId,
                        payloadType = "ROUTE_UPSERT",
                        serializedPayload = route.routeStatus.value,
                        clientEventId = UUID.randomUUID().toString()
                    )
                )
            }
        }
        SyncScheduler.scheduleImmediate(applicationContext)
    }

    override suspend fun getRoute(routeId: String): Route? =
        routeDao.getRoute(routeId)?.toDomain()

    override fun observeRoute(routeId: String): Flow<Route?> =
        routeDao.observeRoute(routeId).map { it?.toDomain() }

    override suspend fun fetchUnsyncedRoutes(): List<Route> =
        routeDao.fetchUnsynced().map { it.toDomain() }

    override suspend fun updateSyncStatus(routeId: String, status: Int) =
        routeDao.updateSyncStatus(routeId, status)

    override suspend fun optimizeRouteStops(routeId: String) {
        val existingRoute = routeDao.getRoute(routeId) ?: return
        val allDeliveries = deliveryDao.getDeliveriesByRoute(routeId, existingRoute.countryCode).map { it.toDomain() }
        val remaining = allDeliveries.filter { it.status != "DELIVERED" && it.status != "FAILED" }
        if (remaining.size < 2) return

        val coordinates = remaining.map { Coordinate(it.latitude, it.longitude) }
        val optimizedIndices = optimizerEngine.compute2Opt(coordinates)
        val reordered = optimizedIndices.mapIndexed { index, originalIndex ->
            remaining[originalIndex].copy(index = index + 1)
        }
        val totalDistance = optimizerEngine.computeTotalDistance(coordinates, optimizedIndices)

        val updatedRoute = existingRoute.toDomain().copy(
            totalDistanceMeters = totalDistance,
            estimatedDurationSeconds = (totalDistance / 8.33).toLong()
        )
        upsertRoute(updatedRoute)
        deliveryDao.upsertDeliveries(reordered.map { it.toEntity() })
    }
}
