package com.smartcourier.core.domain.repository

import com.smartcourier.core.domain.model.User
import com.smartcourier.core.domain.model.UserTier
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeUser(uid: String): Flow<User?>
    suspend fun updateSubscriptionTier(uid: String, tier: UserTier)
    suspend fun clearLocalData()
}
