package com.smartcourier.core.domain.repository

import com.smartcourier.core.domain.model.User

interface UserRepository {
    suspend fun upsertUser(user: User)
    suspend fun fetchUnsyncedUsers(): List<User>
    suspend fun updateSyncStatus(uid: String, status: Int)
}
