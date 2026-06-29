package com.smartcourier.core.data.repository

import android.content.Context
import androidx.room.Room
import com.smartcourier.core.data.local.Database
import com.smartcourier.core.data.local.dao.UserDao
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.domain.model.User
import com.smartcourier.core.domain.model.UserTier
import com.smartcourier.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) : SettingsRepository {

    override fun observeUser(uid: String): Flow<User?> {
        return userDao.observeUser(uid).map { entity -> entity?.toDomain() }
    }

    override suspend fun updateSubscriptionTier(uid: String, tier: UserTier) {
        val current = userDao.getUser(uid) ?: return
        userDao.upsertUser(current.copy(subscriptionTier = tier.name))
    }

    override suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        context.deleteDatabase("smartcourier.db")
    }
}
