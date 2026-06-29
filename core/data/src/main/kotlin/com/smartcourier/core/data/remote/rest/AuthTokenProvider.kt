package com.smartcourier.core.data.remote.rest

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthTokenProvider {
    suspend fun getToken(): String?
}

@Singleton
class FirebaseAuthTokenProvider @Inject constructor(
    private val auth: FirebaseAuth
) : AuthTokenProvider {
    override suspend fun getToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (_: Exception) {
            null
        }
    }
}
