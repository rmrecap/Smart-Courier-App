package com.smartcourier.core.domain.usecase

import com.smartcourier.core.domain.model.Resource
import com.smartcourier.core.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class CompleteDeliveryUseCase(
    private val deliveryRepository: DeliveryRepository
) {
    suspend operator fun invoke(
        deliveryId: String,
        imageUri: String,
        payAmount: Double
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            if (deliveryId.isBlank()) {
                throw IllegalArgumentException("Delivery reference identifier cannot be blank.")
            }
            deliveryRepository.markDeliveryComplete(deliveryId, imageUri, payAmount)
            emit(Resource.Success(Unit))
        } catch (ioException: IOException) {
            emit(Resource.Error(ioException))
        } catch (iae: IllegalArgumentException) {
            emit(Resource.Error(iae))
        }
    }
}
