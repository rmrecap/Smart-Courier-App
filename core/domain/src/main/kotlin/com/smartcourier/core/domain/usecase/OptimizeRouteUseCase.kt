package com.smartcourier.core.domain.usecase

import com.smartcourier.core.domain.model.*
import com.smartcourier.core.domain.repository.DeliveryRepository
import com.smartcourier.core.domain.repository.RouteRepository
import java.util.UUID

class OptimizeRouteUseCase(
    private val optimizerEngine: OptimizerEngine,
    private val routeRepository: RouteRepository,
    private val deliveryRepository: DeliveryRepository
) {
    suspend operator fun invoke(
        userId: String,
        deliveries: List<Delivery>,
        hubCoordinate: Coordinate
    ): Resource<Route> {
        return try {
            val allCoordinates = listOf(hubCoordinate) + deliveries.map { Coordinate(it.latitude, it.longitude) }
            val optimizedIndices = optimizerEngine.compute2Opt(allCoordinates)
            val optimizedDeliveries = optimizedIndices.dropLast(1).drop(1).mapIndexed { index, originalIndex ->
                deliveries[originalIndex - 1].copy(index = index + 1)
            }
            val totalDistance = optimizerEngine.computeTotalDistance(allCoordinates, optimizedIndices)
            val route = Route(
                routeId = UUID.randomUUID().toString(),
                userId = userId,
                routeStatus = RouteStatus.ACTIVE,
                totalDistanceMeters = totalDistance,
                estimatedDurationSeconds = (totalDistance / 8.33).toLong(),
                syncStatus = SYNC_DIRTY,
                deliveries = optimizedDeliveries
            )
            routeRepository.upsertRoute(route)
            deliveryRepository.upsertDeliveries(optimizedDeliveries)
            Resource.Success(route)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }
}
