/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.model.NpmResponse
import com.cmgapps.gradle.model.Package
import com.cmgapps.gradle.service.NetworkService
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.isA
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class CheckNpmPackageActionShould {
    @TempDir
    lateinit var outputDir: Path

    private lateinit var action: CheckNpmPackageAction

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder().build()

        val networkServiceParams =
            object : NetworkService.Params {
                override val baseUrl: Property<String> = project.objects.property<String>().value("https://cmgapps.com")
                override val additionalHeaders: MapProperty<String, String> = project.objects.mapProperty()
                override val engine: Property<HttpClientEngine> =
                    project.objects.property<HttpClientEngine>().value(
                        MockEngine { request ->
                            when {
                                request.url.segments[0] == "my_library" ->
                                    respond(
                                        content =
                                            ByteReadChannel(
                                                Json.encodeToString(
                                                    NpmResponse(
                                                        "my_library",
                                                        "1.0.0",
                                                    ),
                                                ),
                                            ),
                                        status = HttpStatusCode.OK,
                                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                    )

                                else ->
                                    respond(
                                        "Not Found",
                                        HttpStatusCode.NotFound,
                                    )
                            }
                        },
                    )
            }

        val networkService =
            object : NetworkService() {
                override fun getParameters() = networkServiceParams
            }
        action =
            object : CheckNpmPackageAction() {
                override fun getParameters() =
                    object : Params {
                        override val dependencyName = project.objects.property<String>().value("my_library")
                        override val dependencyVersion = project.objects.property<String>().value("1.0.0-alpha.1")
                        override val outputDirectory =
                            project.objects.directoryProperty().apply {
                                set(outputDir.toFile())
                            }
                        override val networkService =
                            project.objects
                                .property<NetworkService>()
                                .value(networkService)
                    }
            }
    }

    @Test
    fun `execute action`() {
        action.execute()

        val content = File(outputDir.toFile(), "my_library.json").readText(Charsets.UTF_8)

        assertThat(content, `is`("""{"name":"my_library","currentVersion":"1.0.0-alpha.1","availableVersion":"1.0.0"}"""))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `save Package model to file`() {
        action.execute()

        val content = Json.decodeFromStream<Package>(File(outputDir.toFile(), "my_library.json").inputStream())

        assertThat(content, isA(Package::class.java))
    }
}
