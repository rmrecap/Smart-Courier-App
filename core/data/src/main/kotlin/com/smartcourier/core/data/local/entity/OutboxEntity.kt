package com.smartcourier.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbox_queue",
    indices = [Index(value = ["timestamp"])]
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: String,
    val payloadType: String,
    val serializedPayload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val clientEventId: String = ""
)
