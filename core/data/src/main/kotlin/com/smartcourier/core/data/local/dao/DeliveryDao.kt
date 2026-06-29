package com.smartcourier.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartcourier.core.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Upsert
    suspend fun upsertDelivery(delivery: DeliveryEntity)

    @Upsert
    suspend fun upsertDeliveries(deliveries: List<DeliveryEntity>)

    @Query("SELECT * FROM deliveries WHERE id = :id")
    suspend fun getDelivery(id: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE routeId = :routeId AND countryCode = :countryCode ORDER BY `index` ASC")
    fun observeByRoute(routeId: String, countryCode: String = "ae"): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE routeId = :routeId AND countryCode = :countryCode ORDER BY `index` ASC")
    suspend fun getDeliveriesByRoute(routeId: String, countryCode: String = "ae"): List<DeliveryEntity>

    @Query("""
        SELECT * FROM deliveries 
        WHERE routeId = :routeId AND status != 'DELIVERED' AND countryCode = :countryCode
        ORDER BY `index` ASC LIMIT 1
    """)
    fun observeNextActiveStop(routeId: String, countryCode: String = "ae"): Flow<DeliveryEntity?>

    @Query("""
        UPDATE deliveries SET 
            status = :status, 
            localPhotoPath = :photoPath, 
            earningsAed = :earnings, 
            syncStatus = 1, 
            versionClock = versionClock + 1,
            lastModifiedTimestamp = :timestamp 
        WHERE id = :id
    """)
    suspend fun markComplete(id: String, status: String, photoPath: String?, earnings: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE deliveries SET photoRemoteUrl = :url, versionClock = versionClock + 1 WHERE id = :id")
    suspend fun updatePhotoRemoteUrl(id: String, url: String?)

    @Query("SELECT * FROM deliveries WHERE syncStatus != 0")
    suspend fun fetchUnsynced(): List<DeliveryEntity>

    @Query("UPDATE deliveries SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: Int)

    @Query("DELETE FROM deliveries WHERE status = 'DELIVERED' AND lastModifiedTimestamp < :cutoff")
    suspend fun deleteCompletedOlderThan(cutoff: Long): Int

    @Query("SELECT COALESCE(SUM(earningsAed), 0) FROM deliveries WHERE status = 'DELIVERED' AND lastModifiedTimestamp >= :since AND countryCode = :countryCode")
    suspend fun sumEarningsSince(since: Long, countryCode: String = "ae"): Double

    @Query("SELECT COALESCE(SUM(tipAmountAed), 0) FROM deliveries WHERE lastModifiedTimestamp >= :since AND countryCode = :countryCode")
    suspend fun sumTipsSince(since: Long, countryCode: String = "ae"): Double

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = 'DELIVERED' AND lastModifiedTimestamp >= :since AND countryCode = :countryCode")
    suspend fun countDeliveredSince(since: Long, countryCode: String = "ae"): Int

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = 'FAILED' AND lastModifiedTimestamp >= :since AND countryCode = :countryCode")
    suspend fun countFailedSince(since: Long, countryCode: String = "ae"): Int

    @Query("SELECT COUNT(*) FROM deliveries WHERE lastModifiedTimestamp >= :since AND countryCode = :countryCode")
    suspend fun countTotalSince(since: Long, countryCode: String = "ae"): Int

    @Query("SELECT * FROM deliveries WHERE status != 'PENDING' AND status != 'IN_TRANSIT' AND countryCode = :countryCode ORDER BY lastModifiedTimestamp DESC")
    fun observeHistoryLedger(countryCode: String = "ae"): Flow<List<DeliveryEntity>>

    @Query("UPDATE deliveries SET tipAmountAed = :amount, versionClock = versionClock + 1, lastModifiedTimestamp = :timestamp WHERE id = :deliveryId")
    suspend fun logTip(deliveryId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM deliveries WHERE lastModifiedTimestamp >= :since AND (status = 'DELIVERED' OR status = 'FAILED') AND countryCode = :countryCode ORDER BY lastModifiedTimestamp DESC")
    fun observeCompletedSince(since: Long, countryCode: String = "ae"): Flow<List<DeliveryEntity>>
}
