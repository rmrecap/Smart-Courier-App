package com.smartcourier.core.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.local.dao.UserDao
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.data.mapper.toDto
import com.smartcourier.core.data.remote.FirestoreDataSource
import com.smartcourier.core.data.remote.NetworkResponse
import com.smartcourier.core.data.remote.StorageDataSource
import com.smartcourier.core.domain.model.SYNC_CLEAN
import com.smartcourier.core.domain.model.SYNC_FAILED
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userDao: UserDao,
    private val routeDao: RouteDao,
    private val deliveryDao: DeliveryDao,
    private val outboxDao: OutboxDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount > 5) {
            Log.w(TAG, "Max retry attempts reached. Marking failed records.")
            markRemainingAsFailed()
            return Result.failure()
        }

        return try {
            log("Sync cycle started (attempt ${runAttemptCount + 1})")

            val outboxProcessed = processOutboxQueue()
            val recordsSynced = syncDirtyRecords()

            log("Sync cycle complete: outbox=$outboxProcessed, dirty=$recordsSynced")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed on attempt ${runAttemptCount + 1}", e)
            Result.retry()
        }
    }

    private suspend fun processOutboxQueue(): Int {
        val pending = outboxDao.getPendingMutations()
        if (pending.isEmpty()) return 0

        var processed = 0
        for (mutation in pending) {
            log("Processing outbox entry ${mutation.id}: type=${mutation.payloadType}, target=${mutation.targetId}")

            val success = when (mutation.payloadType) {
                "DELIVERY_COMPLETE" -> processDeliveryComplete(mutation.targetId, mutation.serializedPayload)
                "DELIVERY_FAILED" -> processDeliveryFailed(mutation.targetId)
                "ROUTE_UPSERT" -> processRouteUpsert(mutation.targetId, mutation.serializedPayload)
                "USER_UPSERT" -> processUserUpsert(mutation.targetId)
                "STOP_STATUS_UPDATE" -> transmitStopStatus(mutation.targetId, mutation.serializedPayload)
                else -> {
                    Log.w(TAG, "Unknown outbox payloadType: ${mutation.payloadType}")
                    false
                }
            }

            if (success) {
                outboxDao.removeById(mutation.id)
                processed++
                log("Outbox entry ${mutation.id} completed and removed")
            }
        }
        return processed
    }

    private suspend fun processDeliveryComplete(deliveryId: String, earningsStr: String): Boolean {
        val delivery = deliveryDao.getDelivery(deliveryId) ?: return false
        val localPhotoPath = delivery.localPhotoPath

        val route = routeDao.getRoute(delivery.routeId)
        val userId = route?.userId ?: return false

        val photoUrl = if (localPhotoPath != null) {
            when (val uploadResult = storageDataSource.uploadProofPhoto(userId, deliveryId, localPhotoPath, delivery.countryCode)) {
                is NetworkResponse.Success -> {
                    deliveryDao.updatePhotoRemoteUrl(deliveryId, uploadResult.data)
                    log("Photo uploaded: ${uploadResult.data}")
                    uploadResult.data
                }
                is NetworkResponse.Failure -> {
                    Log.w(TAG, "Photo upload failed for $deliveryId", uploadResult.exception)
                    null
                }
            }
        } else null

        val dto = delivery.toDomain().toDto().copy(
            photoRemoteUrl = photoUrl,
            expireAt = System.currentTimeMillis() + TTL_24H
        )
        return when (firestoreDataSource.syncDelivery(dto)) {
            is NetworkResponse.Success -> {
                deliveryDao.updateSyncStatus(deliveryId, SYNC_CLEAN)
                firestoreDataSource.syncTrackingToken(
                    trackingToken = delivery.trackingToken,
                    userId = userId,
                    countryCode = delivery.countryCode
                )
                true
            }
            is NetworkResponse.Failure -> false
        }
    }

    private suspend fun processDeliveryFailed(deliveryId: String): Boolean {
        val delivery = deliveryDao.getDelivery(deliveryId) ?: return false
        val dto = delivery.toDomain().toDto().copy(
            expireAt = System.currentTimeMillis() + TTL_24H
        )
        return when (firestoreDataSource.syncDelivery(dto)) {
            is NetworkResponse.Success -> {
                deliveryDao.updateSyncStatus(deliveryId, SYNC_CLEAN)
                true
            }
            is NetworkResponse.Failure -> false
        }
    }

    private suspend fun processRouteUpsert(routeId: String, statusValue: String): Boolean {
        val route = routeDao.getRoute(routeId) ?: return false
        return when (firestoreDataSource.syncRoute(route.toDomain().toDto())) {
            is NetworkResponse.Success -> {
                routeDao.updateSyncStatus(routeId, SYNC_CLEAN)
                true
            }
            is NetworkResponse.Failure -> false
        }
    }

    private suspend fun processUserUpsert(uid: String): Boolean {
        val user = userDao.getUser(uid) ?: return false
        return when (firestoreDataSource.syncUser(user.toDomain().toDto())) {
            is NetworkResponse.Success -> {
                userDao.updateSyncStatus(uid, SYNC_CLEAN)
                true
            }
            is NetworkResponse.Failure -> false
        }
    }

    private suspend fun transmitStopStatus(stopId: String, status: String): Boolean {
        return when (firestoreDataSource.transmitStopStatus(stopId, status)) {
            is NetworkResponse.Success -> true
            is NetworkResponse.Failure -> false
        }
    }

    private suspend fun syncDirtyRecords(): Int {
        var count = 0
        count += syncUsers()
        count += syncRoutes()
        count += syncDeliveries()
        return count
    }

    private suspend fun syncUsers(): Int {
        val unsynced = userDao.fetchUnsynced()
        for (u in unsynced) {
            when (firestoreDataSource.syncUser(u.toDomain().toDto())) {
                is NetworkResponse.Success -> {
                    userDao.updateSyncStatus(u.uid, SYNC_CLEAN)
                }
                is NetworkResponse.Failure -> { }
            }
        }
        return unsynced.size
    }

    private suspend fun syncRoutes(): Int {
        val unsynced = routeDao.fetchUnsynced()
        for (r in unsynced) {
            when (firestoreDataSource.syncRoute(r.toDomain().toDto())) {
                is NetworkResponse.Success -> {
                    routeDao.updateSyncStatus(r.routeId, SYNC_CLEAN)
                }
                is NetworkResponse.Failure -> { }
            }
        }
        return unsynced.size
    }

    private suspend fun syncDeliveries(): Int {
        val unsynced = deliveryDao.fetchUnsynced()
        val expireAt = System.currentTimeMillis() + TTL_24H
        for (d in unsynced) {
            when (firestoreDataSource.syncDelivery(d.toDomain().toDto().copy(expireAt = expireAt))) {
                is NetworkResponse.Success -> {
                    deliveryDao.updateSyncStatus(d.id, SYNC_CLEAN)
                }
                is NetworkResponse.Failure -> { }
            }
        }
        return unsynced.size
    }

    private suspend fun markRemainingAsFailed() {
        deliveryDao.fetchUnsynced().forEach { deliveryDao.updateSyncStatus(it.id, SYNC_FAILED) }
        routeDao.fetchUnsynced().forEach { routeDao.updateSyncStatus(it.routeId, SYNC_FAILED) }
        userDao.fetchUnsynced().forEach { userDao.updateSyncStatus(it.uid, SYNC_FAILED) }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val TTL_24H = 24L * 60 * 60 * 1000
    }
}
