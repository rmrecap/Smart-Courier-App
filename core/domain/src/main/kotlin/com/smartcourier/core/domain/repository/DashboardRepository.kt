package com.smartcourier.core.domain.repository

import com.smartcourier.core.domain.model.DashboardMetrics
import com.smartcourier.core.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeMetrics(userId: String, countryCode: String = "ae"): Flow<DashboardMetrics>
    fun observeActiveRoutes(userId: String, countryCode: String = "ae"): Flow<List<Route>>
}
