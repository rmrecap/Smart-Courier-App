package com.smartcourier.core.domain.repository

import com.smartcourier.core.domain.model.Resource
import com.smartcourier.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun requestOtp(phoneNumber: String): Resource<String>
    suspend fun verifyOtp(verificationId: String, code: String): Resource<User>
    suspend fun isAuthenticated(): Boolean
    suspend fun logout()
}
