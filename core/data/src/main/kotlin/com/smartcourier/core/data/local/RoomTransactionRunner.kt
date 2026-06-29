package com.smartcourier.core.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: Database
) {
    private val transactionMutex = Mutex()

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return transactionMutex.withLock {
            database.withTransaction(block)
        }
    }
}
