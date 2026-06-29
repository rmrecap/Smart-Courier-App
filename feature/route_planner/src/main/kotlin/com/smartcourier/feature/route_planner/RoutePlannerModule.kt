package com.smartcourier.feature.route_planner

import com.smartcourier.core.domain.usecase.OptimizerEngine
import com.smartcourier.core.domain.usecase.ParseAddressesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoutePlannerModule {
    @Provides
    @Singleton
    fun provideOptimizerEngine(): OptimizerEngine = OptimizerEngine()

    @Provides
    @Singleton
    fun provideParseAddressesUseCase(): ParseAddressesUseCase = ParseAddressesUseCase()
}
