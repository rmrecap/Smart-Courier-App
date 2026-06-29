package com.smartcourier.core.data.remote.rest

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.gson.gson
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorClientFactory @Inject constructor(
    private val authTokenProvider: AuthTokenProvider
) {
    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    fun create(): HttpClient = HttpClient {
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

        install(ContentNegotiation) {
            gson(gson)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = TimeUnit.SECONDS.toMillis(15)
            requestTimeoutMillis = TimeUnit.SECONDS.toMillis(30)
            socketTimeoutMillis = TimeUnit.SECONDS.toMillis(15)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val token = authTokenProvider.getToken()
                    if (token != null) {
                        io.ktor.client.plugins.auth.providers.BearerTokens(token, "")
                    } else null
                }
                refreshTokens {
                    val token = authTokenProvider.getToken()
                    if (token != null) {
                        io.ktor.client.plugins.auth.providers.BearerTokens(token, "")
                    } else null
                }
                sendWithoutRequest { request ->
                    request.url.toString().startsWith(ApiRoutes.deliveries)
                }
            }
        }

        install(Logging) {
            level = LogLevel.BODY
        }
    }
}
