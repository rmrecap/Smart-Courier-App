package com.smartcourier.core.domain.model

sealed interface DomainResult<out T> {
    data class Success<out T>(val data: T) : DomainResult<T>
    data class Error(val exception: DataException) : DomainResult<Nothing>
}
