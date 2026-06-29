package com.smartcourier.core.data.repository

import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.domain.model.DashboardMetrics
import com.smartcourier.core.domain.model.Route
import com.smartcourier.core.domain.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstDashboardRepository @Inject constructor(
    private val deliveryDao: DeliveryDao,
    private val routeDao: RouteDao
) : DashboardRepository {

    override fun observeMetrics(userId: String, countryCode: String): Flow<DashboardMetrics> {
        val routesFlow = routeDao.observeByUser(userId, countryCode).map { list ->
            list.filter { it.routeStatus == "ACTIVE" || it.routeStatus == "CREATED" }
        }
        val since = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val summaryFlow = deliveryDao.observeCompletedSince(since, countryCode).map { entities ->
            DashboardMetrics(
                activeRoutes = 0,
                totalDeliveriesToday = entities.size,
                completedToday = entities.count { it.status == "DELIVERED" },
                totalEarningsToday = entities.sumOf { it.earningsAed }
            )
        }
        return combine(routesFlow, summaryFlow) { routes, metrics ->
            metrics.copy(activeRoutes = routes.size)
        }
    }

    override fun observeActiveRoutes(userId: String, countryCode: String): Flow<List<Route>> {
        return routeDao.observeByUser(userId, countryCode).map { entities ->
            entities
                .filter { it.routeStatus == "ACTIVE" || it.routeStatus == "CREATED" }
                .map { it.toDomain() }
        }
    }
}
