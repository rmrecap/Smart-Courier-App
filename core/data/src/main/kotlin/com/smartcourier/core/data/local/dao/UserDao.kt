package com.smartcourier.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartcourier.core.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUser(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid")
    fun observeUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE syncStatus != 0")
    suspend fun fetchUnsynced(): List<UserEntity>

    @Query("UPDATE users SET syncStatus = :status WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, status: Int)
}
