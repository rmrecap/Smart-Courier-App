package com.smartcourier.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.local.dao.UserDao
import com.smartcourier.core.data.local.entity.DeliveryEntity
import com.smartcourier.core.data.local.entity.OutboxEntity
import com.smartcourier.core.data.local.entity.RouteEntity
import com.smartcourier.core.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        RouteEntity::class,
        DeliveryEntity::class,
        OutboxEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun routeDao(): RouteDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL("ALTER TABLE deliveries ADD COLUMN recipientPhone TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_5_6 = Migration(5, 6) { db ->
            db.execSQL("ALTER TABLE deliveries ADD COLUMN tipAmountAed REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE deliveries ADD COLUMN lastModifiedTimestamp INTEGER NOT NULL DEFAULT 0")
        }
    }
}
