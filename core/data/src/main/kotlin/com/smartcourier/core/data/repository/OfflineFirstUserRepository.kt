package com.smartcourier.core.data.repository

import android.content.Context
import com.smartcourier.core.data.local.RoomTransactionRunner
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.dao.UserDao
import com.smartcourier.core.data.local.entity.OutboxEntity
import com.smartcourier.core.data.mapper.toDomain
import com.smartcourier.core.data.mapper.toEntity
import com.smartcourier.core.data.sync.SyncScheduler
import com.smartcourier.core.domain.model.User
import com.smartcourier.core.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstUserRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val userDao: UserDao,
    private val outboxDao: OutboxDao,
    private val transactionRunner: RoomTransactionRunner
) : UserRepository {

    override suspend fun upsertUser(user: User) {
        val existing = userDao.getUser(user.uid)
        val entity = user.toEntity().copy(versionClock = (existing?.versionClock ?: 0) + 1)
        transactionRunner.runInTransaction {
            userDao.upsertUser(entity)
            if (outboxDao.countPending(user.uid, "USER_UPSERT") == 0) {
                outboxDao.enqueue(
                    OutboxEntity(
                        targetId = user.uid,
                        payloadType = "USER_UPSERT",
                        serializedPayload = user.subscriptionTier.name,
                        clientEventId = UUID.randomUUID().toString()
                    )
                )
            }
        }
        SyncScheduler.scheduleImmediate(applicationContext)
    }

    override suspend fun fetchUnsyncedUsers(): List<User> =
        userDao.fetchUnsynced().map { it.toDomain() }

    override suspend fun updateSyncStatus(uid: String, status: Int) =
        userDao.updateSyncStatus(uid, status)
}
