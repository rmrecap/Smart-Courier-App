package com.smartcourier.core.domain.usecase

import kotlinx.coroutines.flow.Flow

interface FlowUseCase<in Input, out Output> {
    operator fun invoke(params: Input): Flow<Output>
}
