package com.smartcourier.core.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val UNIQUE_IMMEDIATE_WORK = "smartcourier_sync_immediate"
    private const val UNIQUE_PERIODIC_WORK = "smartcourier_sync_periodic"
    private const val UNIQUE_PURGE_WORK = "smartcourier_local_purge"
    private const val PERIODIC_INTERVAL_MINUTES = 15L
    private const val PURGE_INTERVAL_HOURS = 6L

    fun scheduleImmediate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag("sync_immediate")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag("sync_periodic")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun schedulePeriodicPurge(context: Context) {
        val request = PeriodicWorkRequestBuilder<LocalDatabasePurgeWorker>(PURGE_INTERVAL_HOURS, TimeUnit.HOURS)
            .addTag("purge_periodic")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PURGE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_IMMEDIATE_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PURGE_WORK)
    }
}
