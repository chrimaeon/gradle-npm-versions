/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkServiceShould {
    private lateinit var networkServiceParams: NetworkService.Params
    private lateinit var networkService: NetworkService

    private lateinit var baseUrlProperty: Property<String>

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder().build()
        baseUrlProperty = project.objects.property(String::class.java).convention("https://localhost")
        networkServiceParams =
            object : NetworkService.Params {
                override val baseUrl: Property<String>
                    get() = baseUrlProperty
                override val additionalHeaders: MapProperty<String, String>
                    get() = project.objects.mapProperty(String::class.java, String::class.java)
                override val engine: Property<HttpClientEngine>
                    get() =
                        project.objects.property(HttpClientEngine::class.java).convention(
                            MockEngine {
                                respondOk()
                            },
                        )
            }
        networkService =
            object : NetworkService() {
                override fun getParameters(): Params = networkServiceParams
            }
    }

    @Test
    fun `set base url`() =
        runTest {
            baseUrlProperty.set("https://cmgapps.com")

            val response = networkService.get("")

            assertThat(response.request.url.host, `is`("cmgapps.com"))
        }

    @Test
    fun `add path parameters`() =
        runTest {
            val response = networkService.get("path1", "path2")

            assertThat(response.request.url.segments, contains("path1", "path2"))
        }

    @Test
    fun `add additional headers`() =
        runTest {
            networkServiceParams.additionalHeaders.put(HttpHeaders.Accept, "application/json")

            val response = networkService.get("")

            assertThat(response.request.headers[HttpHeaders.Accept], `is`("application/json"))
        }
}
