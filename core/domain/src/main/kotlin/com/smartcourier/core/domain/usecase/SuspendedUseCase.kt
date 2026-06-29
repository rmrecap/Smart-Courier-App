package com.smartcourier.core.domain.usecase

interface SuspendedUseCase<in Input, out Output> {
    suspend operator fun invoke(params: Input): Output
}
