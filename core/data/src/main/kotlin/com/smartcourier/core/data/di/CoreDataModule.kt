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
import com.smartcourier.core.data.telemetry.RemoteTelemetryDataSource
import okhttp3.OkHttpClient
import com.smartcourier.core.data.repository.AuthRepositoryImpl
import com.smartcourier.core.data.repository.OfflineFirstDashboardRepository
import com.smartcourier.core.data.repository.OfflineFirstDeliveryRepository
import com.smartcourier.core.data.repository.OfflineFirstRouteRepository
import com.smartcourier.core.data.repository.OfflineFirstSettingsRepository
import com.smartcourier.core.data.repository.OfflineFirstUserRepository
import com.smartcourier.core.data.sync.ConnectivityMonitor
import com.smartcourier.core.data.sync.SyncScheduler
import com.smartcourier.core.data.sync.PurgeSchedulerInit
import com.smartcourier.core.domain.repository.AuthRepository
import com.smartcourier.core.domain.repository.DashboardRepository
import com.smartcourier.core.domain.repository.DeliveryRepository
import com.smartcourier.core.domain.repository.RouteRepository
import com.smartcourier.core.domain.repository.SettingsRepository
import com.smartcourier.core.domain.repository.UserRepository
import com.smartcourier.core.domain.usecase.CompleteDeliveryUseCase
import com.smartcourier.core.domain.usecase.OptimizeRouteUseCase
import com.smartcourier.core.domain.usecase.OptimizerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(
            context,
            Database::class.java,
            "smartcourier.db"
        )
            .addMigrations(Database.MIGRATION_4_5, Database.MIGRATION_5_6)
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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideFirestoreDataSource(firestore: FirebaseFirestore): FirestoreDataSource =
        FirestoreDataSource(firestore)

    @Provides
    @Singleton
    fun provideRemoteTelemetryDataSource(
        auth: FirebaseAuth,
        client: OkHttpClient
    ): RemoteTelemetryDataSource = RemoteTelemetryDataSource(auth, client)

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
    fun providePeriodicPurgeScheduler(@ApplicationContext context: Context): PurgeSchedulerInit {
        SyncScheduler.schedulePeriodicPurge(context)
        return PurgeSchedulerInit()
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(
        deliveryDao: DeliveryDao,
        routeDao: RouteDao
    ): DashboardRepository = OfflineFirstDashboardRepository(deliveryDao, routeDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
        userDao: UserDao
    ): SettingsRepository = OfflineFirstSettingsRepository(context, userDao)

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository =
        AuthRepositoryImpl(auth)

    @Provides
    @Singleton
    fun provideCompleteDeliveryUseCase(
        deliveryRepository: DeliveryRepository
    ): CompleteDeliveryUseCase = CompleteDeliveryUseCase(deliveryRepository)

    @Provides
    @Singleton
    fun provideOptimizeRouteUseCase(
        optimizerEngine: OptimizerEngine,
        routeRepository: RouteRepository,
        deliveryRepository: DeliveryRepository
    ): OptimizeRouteUseCase = OptimizeRouteUseCase(optimizerEngine, routeRepository, deliveryRepository)
}
