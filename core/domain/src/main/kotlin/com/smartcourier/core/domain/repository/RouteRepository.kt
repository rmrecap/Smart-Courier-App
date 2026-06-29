package com.smartcourier.core.domain.repository

import com.smartcourier.core.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    suspend fun upsertRoute(route: Route)
    suspend fun getRoute(routeId: String): Route?
    fun observeRoute(routeId: String): Flow<Route?>
    suspend fun fetchUnsyncedRoutes(): List<Route>
    suspend fun updateSyncStatus(routeId: String, status: Int)
    suspend fun optimizeRouteStops(routeId: String)
}
