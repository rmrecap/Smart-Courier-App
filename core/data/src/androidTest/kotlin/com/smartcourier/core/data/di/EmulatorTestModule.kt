package com.smartcourier.core.data.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.smartcourier.core.data.local.Database
import com.smartcourier.core.data.local.RoomTransactionRunner
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.local.dao.UserDao
import com.smartcourier.core.data.remote.FirestoreDataSource
import com.smartcourier.core.data.remote.StorageDataSource
import com.smartcourier.core.data.repository.OfflineFirstDeliveryRepository
import com.smartcourier.core.data.repository.OfflineFirstRouteRepository
import com.smartcourier.core.data.repository.OfflineFirstUserRepository
import com.smartcourier.core.data.sync.ConnectivityMonitor
import com.smartcourier.core.domain.repository.DeliveryRepository
import com.smartcourier.core.domain.repository.RouteRepository
import com.smartcourier.core.domain.repository.UserRepository
import com.smartcourier.core.domain.usecase.CompleteDeliveryUseCase
import com.smartcourier.core.domain.usecase.OptimizerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreDataModule::class]
)
object EmulatorTestModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Database {
        return Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .build()
    }

    @Provides
    fun provideUserDao(db: Database): UserDao = db.userDao()

    @Provides
    fun provideRouteDao(db: Database): RouteDao = db.routeDao()

    @Provides
    fun provideDeliveryDao(db: Database): DeliveryDao = db.deliveryDao()

    @Provides
    fun provideOutboxDao(db: Database): OutboxDao = db.outboxDao()

    @Provides
    @Singleton
    fun provideRoomTransactionRunner(database: Database): RoomTransactionRunner =
        RoomTransactionRunner(database)

    @Provides
    @Singleton
    fun provideConnectivityMonitor(@ApplicationContext context: Context): ConnectivityMonitor =
        ConnectivityMonitor(context)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        try {
            db.useEmulator("10.0.2.2", 8080)
        } catch (_: IllegalStateException) { }
        return db
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = FirebaseStorage.getInstance()
        try {
            storage.useEmulator("10.0.2.2", 9199)
        } catch (_: IllegalStateException) { }
        return storage
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        try {
            auth.useEmulator("10.0.2.2", 9099)
        } catch (_: IllegalStateException) { }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirestoreDataSource(firestore: FirebaseFirestore): FirestoreDataSource =
        FirestoreDataSource(firestore)

    @Provides
    @Singleton
    fun provideStorageDataSource(
        storage: FirebaseStorage,
        @ApplicationContext context: Context
    ): StorageDataSource = StorageDataSource(storage, context)

    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context,
        userDao: UserDao,
        outboxDao: OutboxDao,
        transactionRunner: RoomTransactionRunner
    ): UserRepository = OfflineFirstUserRepository(context, userDao, outboxDao, transactionRunner)

    @Provides
    @Singleton
    fun provideOptimizerEngine(): OptimizerEngine = OptimizerEngine()

    @Provides
    @Singleton
    fun provideRouteRepository(
        @ApplicationContext context: Context,
        routeDao: RouteDao,
        deliveryDao: DeliveryDao,
        outboxDao: OutboxDao,
        transactionRunner: RoomTransactionRunner,
        optimizerEngine: OptimizerEngine
    ): RouteRepository = OfflineFirstRouteRepository(context, routeDao, deliveryDao, outboxDao, transactionRunner, optimizerEngine)

    @Provides
    @Singleton
    fun provideDeliveryRepository(
        @ApplicationContext context: Context,
        deliveryDao: DeliveryDao,
        outboxDao: OutboxDao,
        transactionRunner: RoomTransactionRunner
    ): DeliveryRepository = OfflineFirstDeliveryRepository(context, deliveryDao, outboxDao, transactionRunner)

    @Provides
    @Singleton
    fun provideCompleteDeliveryUseCase(
        deliveryRepository: DeliveryRepository
    ): CompleteDeliveryUseCase = CompleteDeliveryUseCase(deliveryRepository)

}
