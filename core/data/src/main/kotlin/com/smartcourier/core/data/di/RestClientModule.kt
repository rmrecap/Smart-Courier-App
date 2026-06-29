package com.smartcourier.core.data.di

import com.google.firebase.auth.FirebaseAuth
import com.smartcourier.core.data.remote.rest.AuthTokenProvider
import com.smartcourier.core.data.remote.rest.FirebaseAuthTokenProvider
import com.smartcourier.core.data.remote.rest.KtorClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RestClientModule {

    @Provides
    @Singleton
    fun provideAuthTokenProvider(auth: FirebaseAuth): AuthTokenProvider =
        FirebaseAuthTokenProvider(auth)

    @Provides
    @Singleton
    fun provideKtorClientFactory(authTokenProvider: AuthTokenProvider): KtorClientFactory =
        KtorClientFactory(authTokenProvider)

    @Provides
    @Singleton
    fun provideHttpClient(factory: KtorClientFactory): HttpClient = factory.create()
}
