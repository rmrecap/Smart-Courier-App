package com.smartcourier.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.smartcourier.core.data.local.entity.OutboxEntity

@Dao
interface OutboxDao {
    @Insert
    suspend fun enqueue(entry: OutboxEntity)

    @Query("SELECT COUNT(*) FROM outbox_queue WHERE targetId = :targetId AND payloadType = :payloadType")
    suspend fun countPending(targetId: String, payloadType: String): Int

    @Query("SELECT COUNT(*) FROM outbox_queue WHERE clientEventId = :clientEventId")
    suspend fun existsByClientEventId(clientEventId: String): Int

    @Query("SELECT * FROM outbox_queue ORDER BY timestamp ASC")
    suspend fun getPendingMutations(): List<OutboxEntity>

    @Query("DELETE FROM outbox_queue WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("DELETE FROM outbox_queue WHERE targetId = :targetId AND payloadType = :payloadType")
    suspend fun removeByTarget(targetId: String, payloadType: String)
}
