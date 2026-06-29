package com.smartcourier.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartcourier.core.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Upsert
    suspend fun upsertRoute(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    suspend fun getRoute(routeId: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    fun observeRoute(routeId: String): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE countryCode = :countryCode")
    fun observeAll(countryCode: String = "ae"): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE userId = :userId AND countryCode = :countryCode")
    fun observeByUser(userId: String, countryCode: String = "ae"): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE syncStatus != 0")
    suspend fun fetchUnsynced(): List<RouteEntity>

    @Query("UPDATE routes SET syncStatus = :status WHERE routeId = :routeId")
    suspend fun updateSyncStatus(routeId: String, status: Int)
}
