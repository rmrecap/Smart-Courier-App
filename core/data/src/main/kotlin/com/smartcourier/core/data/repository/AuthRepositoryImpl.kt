package com.smartcourier.core.data.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.smartcourier.core.domain.model.Resource
import com.smartcourier.core.domain.model.User
import com.smartcourier.core.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun requestOtp(phoneNumber: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInAnonymously().await()
            if (authResult.user != null) {
                Resource.Success("anonymous")
            } else {
                Resource.Error(Exception("Anonymous sign-in returned null user"))
            }
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun verifyOtp(verificationId: String, code: String): Resource<User> = withContext(Dispatchers.IO) {
        try {
            when (verificationId) {
                "anonymous" -> {
                    val firebaseUser = auth.currentUser
                        ?: return@withContext Resource.Error(Exception("No anonymous user found"))
                    Resource.Success(
                        User(
                            uid = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "Rider",
                            email = firebaseUser.email ?: "",
                            countryCode = "ae",
                            lastModifiedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
                else -> {
                    val credential = PhoneAuthProvider.getCredential(verificationId, code)
                    val authResult = auth.signInWithCredential(credential).await()
                    val firebaseUser = authResult.user
                        ?: return@withContext Resource.Error(Exception("Phone sign-in returned null user"))
                    Resource.Success(
                        User(
                            uid = firebaseUser.uid,
                            name = firebaseUser.phoneNumber ?: "Rider",
                            email = firebaseUser.email ?: "",
                            countryCode = extractCountryCode(firebaseUser.phoneNumber ?: ""),
                            lastModifiedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        auth.currentUser != null
    }

    override suspend fun logout() {
        auth.signOut()
    }

    fun createPhoneAuthOptions(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    private fun extractCountryCode(phoneNumber: String): String {
        return when {
            phoneNumber.startsWith("+971") -> "ae"
            phoneNumber.startsWith("+966") -> "sa"
            phoneNumber.startsWith("+968") -> "om"
            phoneNumber.startsWith("+92") -> "pk"
            phoneNumber.startsWith("+63") -> "ph"
            else -> "ae"
        }
    }
}
