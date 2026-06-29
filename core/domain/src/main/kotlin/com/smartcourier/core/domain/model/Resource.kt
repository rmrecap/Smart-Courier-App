package com.smartcourier.core.domain.model

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val exception: Throwable) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
