package com.smartcourier.core.data.di

import android.content.Context
import com.smartcourier.core.data.local.RoomTransactionRunner
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.remote.rest.RestDeliveryRepository
import com.smartcourier.core.domain.repository.DeliveryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryBindingModule {

    /*
     * ─── Pluggable Repository Swap Point ───
     *
     * To switch from Firebase to REST backend:
     *
     * 1. Remove `provideDeliveryRepository` from CoreDataModule.kt
     * 2. Delete the @RestRepository qualifier from the function below
     * 3. Hilt will resolve DeliveryRepository → RestDeliveryRepository automatically
     *
     * To keep both implementations available simultaneously:
     *   - @RestRepository → RestDeliveryRepository (this module)
     *   - default binding → OfflineFirstDeliveryRepository (CoreDataModule)
     *
     * Consumers inject a specific implementation by adding/removing @RestRepository.
     */

    @Provides
    @Singleton
    @RestRepository
    fun provideRestDeliveryRepository(
        @ApplicationContext context: Context,
        deliveryDao: DeliveryDao,
        outboxDao: OutboxDao,
        transactionRunner: RoomTransactionRunner,
        httpClient: HttpClient
    ): DeliveryRepository = RestDeliveryRepository(
        applicationContext = context,
        deliveryDao = deliveryDao,
        outboxDao = outboxDao,
        transactionRunner = transactionRunner,
        httpClient = httpClient
    )
}
