package com.smartcourier.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NetworkResponse<out T> {
    data class Success<out T>(val data: T) : NetworkResponse<T>
    data class Failure(val exception: Throwable) : NetworkResponse<Nothing>
}

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun syncUser(userDto: UserDto): NetworkResponse<Unit> = withContext(Dispatchers.IO) {
        retryRequest {
            try {
                firestore.collection("users").document(userDto.uid).set(userDto).await()
                NetworkResponse.Success(Unit)
            } catch (e: Exception) {
                NetworkResponse.Failure(e)
            }
        }
    }

    suspend fun syncRoute(routeDto: RouteDto): NetworkResponse<Unit> = withContext(Dispatchers.IO) {
        retryRequest {
            try {
                firestore.collection("routes").document(routeDto.routeId).set(routeDto).await()
                NetworkResponse.Success(Unit)
            } catch (e: Exception) {
                NetworkResponse.Failure(e)
            }
        }
    }

    suspend fun syncDelivery(deliveryDto: DeliveryDto): NetworkResponse<Unit> = withContext(Dispatchers.IO) {
        retryRequest {
            try {
                firestore.collection("deliveries").document(deliveryDto.id).set(deliveryDto).await()
                NetworkResponse.Success(Unit)
            } catch (e: Exception) {
                NetworkResponse.Failure(e)
            }
        }
    }

    suspend fun syncTrackingToken(
        trackingToken: String,
        userId: String,
        countryCode: String
    ): NetworkResponse<Unit> = withContext(Dispatchers.IO) {
        retryRequest {
            try {
                firestore.collection("tracking").document(trackingToken)
                    .set(mapOf("userId" to userId, "countryCode" to countryCode))
                    .await()
                NetworkResponse.Success(Unit)
            } catch (e: Exception) {
                NetworkResponse.Failure(e)
            }
        }
    }

    suspend fun transmitStopStatus(stopId: String, status: String): NetworkResponse<Unit> = withContext(Dispatchers.IO) {
        retryRequest {
            try {
                firestore.collection("stops").document(stopId)
                    .set(mapOf("status" to status), SetOptions.merge())
                    .await()
                NetworkResponse.Success(Unit)
            } catch (e: Exception) {
                NetworkResponse.Failure(e)
            }
        }
    }

    private suspend fun <T> retryRequest(
        times: Int = 3,
        initialDelay: Long = 1000,
        block: suspend () -> NetworkResponse<T>
    ): NetworkResponse<T> {
        var currentDelay = initialDelay
        repeat(times - 1) {
            val response = block()
            if (response is NetworkResponse.Success) return response
            delay(currentDelay)
            currentDelay *= 2
        }
        return block()
    }
}
