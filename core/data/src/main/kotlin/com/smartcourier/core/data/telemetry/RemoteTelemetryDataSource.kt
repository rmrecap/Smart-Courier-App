package com.smartcourier.core.data.telemetry

import com.google.firebase.auth.FirebaseAuth
import com.smartcourier.core.data.remote.NetworkResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class TelemetryPayload(
    val c: String,
    val b: Int,
    val h: Int,
    val t: Long
)

@Singleton
class RemoteTelemetryDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val client: OkHttpClient
) {
    private val baseUrl = "https://smart-courier-app-e8624-default-rtdb.firebaseio.com"
    private val jsonMediaType = "application/json".toMediaType()
    private val gson = Gson()

    suspend fun updateLocation(
        countryCode: String,
        userId: String,
        payload: TelemetryPayload
    ): NetworkResponse<Unit> = withContext(Dispatchers.IO) {
        val idToken = runCatching {
            auth.currentUser?.getIdToken(true)?.await()?.token
        }.getOrNull() ?: return@withContext NetworkResponse.Failure(
            Exception("Not authenticated")
        )

        val url = "${baseUrl}/telemetry/${countryCode}/${userId}.json?auth=${idToken}"
        val json = gson.toJson(payload)
        val body = json.toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).put(body).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) NetworkResponse.Success(Unit)
            else NetworkResponse.Failure(Exception("RTDB PUT failed: ${response.code} ${response.message}"))
        } catch (e: Exception) {
            NetworkResponse.Failure(e)
        }
    }

    suspend fun readLocation(
        countryCode: String,
        userId: String
    ): NetworkResponse<TelemetryPayload> = withContext(Dispatchers.IO) {
        val idToken = runCatching {
            auth.currentUser?.getIdToken(true)?.await()?.token
        }.getOrNull() ?: return@withContext NetworkResponse.Failure(
            Exception("Not authenticated")
        )

        val url = "${baseUrl}/telemetry/${countryCode}/${userId}.json?auth=${idToken}"
        val request = Request.Builder().url(url).get().build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body.isNullOrBlank() || body == "null") {
                    return@withContext NetworkResponse.Failure(Exception("No telemetry data"))
                }
                val payload = gson.fromJson(body, TelemetryPayload::class.java)
                if (payload != null) NetworkResponse.Success(payload)
                else NetworkResponse.Failure(Exception("Failed to parse telemetry"))
            } else {
                NetworkResponse.Failure(Exception("RTDB GET failed: ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            NetworkResponse.Failure(e)
        }
    }
}
