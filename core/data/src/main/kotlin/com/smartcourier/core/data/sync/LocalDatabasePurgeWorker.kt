package com.smartcourier.core.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.local.dao.UserDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocalDatabasePurgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val deliveryDao: DeliveryDao,
    private val routeDao: RouteDao,
    private val userDao: UserDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        log("Purging records older than $cutoff")

        val deletedDeliveries = deliveryDao.deleteCompletedOlderThan(cutoff)
        log("Purged $deletedDeliveries completed deliveries")

        return Result.success()
    }

    private fun log(msg: String) = Log.d(TAG, msg)

    companion object {
        private const val TAG = "LocalPurgeWorker"
        private const val RETENTION_MS = 24L * 60 * 60 * 1000
    }
}
