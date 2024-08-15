/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.appendPathSegments
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.LoggerFactory

abstract class NetworkService : BuildService<NetworkService.Params> {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val client: HttpClient
        get() =
            HttpClient(parameters.engine.getOrElse(Apache5.create())) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
                defaultRequest {
                    url(parameters.baseUrl.get())
                    headers.appendAll(
                        headers {
                            parameters.additionalHeaders.get().forEach(this::append)
                        },
                    )
                }
                install(Logging) {
                    this.level =
                        when {
                            this@NetworkService.logger.isTraceEnabled -> LogLevel.ALL
                            this@NetworkService.logger.isDebugEnabled -> LogLevel.BODY
                            this@NetworkService.logger.isInfoEnabled -> LogLevel.HEADERS
                            else -> LogLevel.NONE
                        }
                    this.logger =
                        object : Logger {
                            override fun log(message: String) {
                                this@NetworkService.logger.info(message)
                            }
                        }
                    this.sanitizeHeader { header -> header == HttpHeaders.Authorization }
                }
            }

    suspend fun get(vararg pathSegments: String): HttpResponse =
        client.get {
            url {
                appendPathSegments(*pathSegments)
            }
        }

    interface Params : BuildServiceParameters {
        val baseUrl: Property<String>
        val additionalHeaders: MapProperty<String, String>
        val engine: Property<HttpClientEngine>
    }
}
