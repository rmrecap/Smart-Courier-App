package com.smartcourier.core.domain.model

sealed class DataException(override val message: String) : Exception(message) {
    data object NetworkUnavailable : DataException("No network connection available")
    data object UnauthorizedAccess : DataException("Access denied")
    data object DiskFull : DataException("Local storage is full")
    data class ValidationError(override val message: String) : DataException(message)
    data class Unknown(val source: Throwable) : DataException(source.message ?: "Unknown error")

    companion object {
        fun fromThrowable(t: Throwable): DataException = when (t) {
            is java.net.UnknownHostException -> NetworkUnavailable
            is java.net.SocketTimeoutException -> NetworkUnavailable
            is DataException -> t
            else -> Unknown(t)
        }
    }
}
