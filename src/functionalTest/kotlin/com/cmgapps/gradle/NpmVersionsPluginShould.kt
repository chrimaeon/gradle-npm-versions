/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.util.assertExpectedFiles
import com.cmgapps.gradle.util.createBuildRunner
import com.cmgapps.gradle.util.fixturesDir
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedInvocationConstants
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class NpmVersionsPluginShould {
    private lateinit var server: EmbeddedServer<*, *>

    @BeforeEach
    fun setUp() {
        server =
            embeddedServer(CIO, port = 8080) {
                install(ContentNegotiation) { json() }
                routing {
                    get("{packageName}/latest") {
                        val pkg = call.parameters.getOrFail<String>("packageName")
                        when (pkg) {
                            "bootstrap" -> {
                                call.respond(
                                    buildJsonObject {
                                        put("name", pkg)
                                        put("version", "5.3.8")
                                    },
                                )
                            }

                            "kotlin" -> {
                                call.respond(
                                    buildJsonObject {
                                        put("name", pkg)
                                        put("version", "1.0.0")
                                    },
                                )
                            }

                            else -> {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        }
                    }
                }
            }.start(wait = false)
    }

    @AfterEach
    fun stopServer() {
        server.stop(gracePeriodMillis = 0, timeoutMillis = 0)
    }

    @ParameterizedTest(name = "${ParameterizedInvocationConstants.DISPLAY_NAME_PLACEHOLDER} - Gradle Version = {0}")
    @MethodSource("gradleVersions")
    fun `apply NpmVersions plugin to various Gradle versions`(version: String) {
        val fixturesDir = fixturesDir.resolve("report-npm-packages")
        val result =
            createBuildRunner(fixturesDir)
                .apply {
                    if (version != LATEST_VERSION) {
                        withGradleVersion(version)
                    }
                }.build()

        assertThat(
            "Gradle version $version",
            result.task(":npmVersions")?.outcome,
            `is`(TaskOutcome.SUCCESS),
        )
    }

    @Test
    fun `report npm packages`() {
        val fixturesDir = fixturesDir.resolve("report-npm-packages")
        createBuildRunner(fixturesDir).build()

        assertExpectedFiles(fixturesDir)
    }

    @Test
    fun `report all reports`() {
        val fixturesDir = fixturesDir.resolve("report-all-reports")
        createBuildRunner(fixturesDir).build()

        assertExpectedFiles(fixturesDir)
    }

    companion object {
        @JvmStatic
        fun gradleVersions(): Stream<Arguments> =
            buildList {
                add(MINIMUM_GRADLE_VERSION)
                add(LATEST_VERSION)
                if (System.getenv("CI") == null) {
                    add("9.1.0")
                    add("9.2.0")
                    add("9.3.0")
                    add("9.4.0")
                    add("9.5.0")
                }
            }.stream().map { arguments(it) }
    }
}

private const val LATEST_VERSION = "latest"
