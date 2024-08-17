/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.model.NpmResponse
import com.cmgapps.gradle.reporter.asString
import com.cmgapps.gradle.service.NetworkService
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.workers.ClassLoaderWorkerSpec
import org.gradle.workers.ProcessWorkerSpec
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.gradle.workers.WorkerSpec
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

private const val TEST_DEP_NAME = "npm-dependency"
private const val TEST_DEP_VERSION = "1.0.0"

class NpmVersionTaskShould {
    @TempDir
    lateinit var testProjectDir: Path

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project =
            ProjectBuilder
                .builder()
                .withProjectDir(testProjectDir.toFile())
                .build()

        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
    }

    @Test
    fun `create console report`() {
        val outputDir = testProjectDir.resolve("output")
        val task =
            project.tasks.create(
                "npmVersions",
                NpmVersionTask::class.java,
                TestWorkExecutor(project),
                project.objects,
            )

        val configuration = project.configurations.create("implementation")

        project.dependencies.add(
            "implementation",
            NpmDependency(
                objectFactory = project.objects,
                name = TEST_DEP_NAME,
                version = TEST_DEP_VERSION,
            ),
        )

        task.outputDirectory.set(outputDir.toFile().apply { mkdirs() })
        task.configurationToCheck(
            project.provider {
                configuration
            },
        )

        val outputStream = ByteArrayOutputStream()

        System.setOut(PrintStream(outputStream))

        task.action()

        assertThat(
            outputStream.asString(),
            `is`(
                "┌──────────────┐\n" +
                    "│ NPM Packages │\n" +
                    "└──────────────┘\n" +
                    "\n" +
                    "The following packages are using the latest version:\n" +
                    " • npm-dependency:1.0.0\n",
            ),
        )
    }

    @Test
    fun `create plain text report`() {
        val outputDir = testProjectDir.resolve("output")
        val task =
            project.tasks.create(
                "npmVersions",
                NpmVersionTask::class.java,
                TestWorkExecutor(project),
                project.objects,
            )

        val configuration = project.configurations.create("implementation")

        project.dependencies.add(
            "implementation",
            NpmDependency(
                objectFactory = project.objects,
                name = TEST_DEP_NAME,
                version = TEST_DEP_VERSION,
            ),
        )

        task.outputDirectory.set(outputDir.toFile().apply { mkdirs() })

        val reportOutputFile = outputDir.resolve("report.txt")

        task.reports.plainText.required
            .set(true)
        task.reports.plainText.outputLocation
            .set(reportOutputFile.toFile())

        task.configurationToCheck(
            project.provider {
                configuration
            },
        )

        task.action()

        assertThat(
            reportOutputFile.toFile().readText(),
            `is`(
                "┌──────────────┐\n" +
                    "│ NPM Packages │\n" +
                    "└──────────────┘\n" +
                    "\n" +
                    "The following packages are using the latest version:\n" +
                    " • npm-dependency:1.0.0\n",
            ),
        )
    }

    @Test
    fun `create json report`() {
        val outputDir = testProjectDir.resolve("output")
        val task =
            project.tasks.create(
                "npmVersions",
                NpmVersionTask::class.java,
                TestWorkExecutor(project),
                project.objects,
            )

        val configuration = project.configurations.create("implementation")

        project.dependencies.add(
            "implementation",
            NpmDependency(
                objectFactory = project.objects,
                name = TEST_DEP_NAME,
                version = TEST_DEP_VERSION,
            ),
        )

        task.outputDirectory.set(outputDir.toFile().apply { mkdirs() })

        val reportOutputFile = outputDir.resolve("report.json")

        task.reports.json.required
            .set(true)
        task.reports.json.outputLocation
            .set(reportOutputFile.toFile())

        task.configurationToCheck(
            project.provider {
                configuration
            },
        )

        task.action()

        assertThat(
            reportOutputFile.toFile().readText(),
            `is`(
                "{\n" +
                    "    \"latest\": [\n" +
                    "        {\n" +
                    "            \"name\": \"npm-dependency\",\n" +
                    "            \"version\": \"1.0.0\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"outdated\": []\n" +
                    "}\n",
            ),
        )
    }

    @Test
    fun `create html report`() {
        val outputDir = testProjectDir.resolve("output")
        val task =
            project.tasks.create(
                "npmVersions",
                NpmVersionTask::class.java,
                TestWorkExecutor(project),
                project.objects,
            )

        val configuration = project.configurations.create("implementation")

        project.dependencies.add(
            "implementation",
            NpmDependency(
                objectFactory = project.objects,
                name = TEST_DEP_NAME,
                version = TEST_DEP_VERSION,
            ),
        )

        task.outputDirectory.set(outputDir.toFile().apply { mkdirs() })

        val reportOutputFile = outputDir.resolve("report.html")

        task.reports.html.required
            .set(true)
        task.reports.html.outputLocation
            .set(reportOutputFile.toFile())

        task.configurationToCheck(
            project.provider {
                configuration
            },
        )

        task.action()

        val normalizeCss = javaClass.getResourceAsStream("/normalize.css")!!.bufferedReader().use { it.readText() }
        val styleCss = javaClass.getResourceAsStream("/style.css")!!.bufferedReader().use { it.readText() }

        val expected =
            "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <title>\n" +
                "      NPM Versions\n" +
                "    </title>\n" +
                "    <style>\n" +
                "      " + normalizeCss +
                "\n" +
                "    </style>\n" +
                "    <style>\n" +
                "      " + styleCss +
                "\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <h1>\n" +
                "      NPM Versions\n" +
                "    </h1>\n" +
                "    <p>\n" +
                "      The following packages are using the latest version\n" +
                "    </p>\n" +
                "    <table>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          $TEST_DEP_NAME\n" +
                "        </td>\n" +
                "        <td>\n" +
                "          $TEST_DEP_VERSION\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "    <p style=\"text-align:right\">\n" +
                "      <small>\n" +
                "        Generated with \n" +
                "        <a href=\"https://plugins.gradle.org/plugin/com.cmgapps.npm.versions\">NPM Versions Gradle Plugin</a>\n" +
                "      </small>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>\n"

        assertThat(
            reportOutputFile.toFile().readText(),
            `is`(expected),
        )
    }

    @Test
    fun `create xml report`() {
        val outputDir = testProjectDir.resolve("output")
        val task =
            project.tasks.create(
                "npmVersions",
                NpmVersionTask::class.java,
                TestWorkExecutor(project),
                project.objects,
            )

        val configuration = project.configurations.create("implementation")

        project.dependencies.add(
            "implementation",
            NpmDependency(
                objectFactory = project.objects,
                name = TEST_DEP_NAME,
                version = TEST_DEP_VERSION,
            ),
        )

        task.outputDirectory.set(outputDir.toFile().apply { mkdirs() })

        val reportOutputFile = outputDir.resolve("report.xml")

        task.reports.xml.required
            .set(true)
        task.reports.xml.outputLocation
            .set(reportOutputFile.toFile())

        task.configurationToCheck(
            project.provider {
                configuration
            },
        )

        task.action()

        @Suppress("ktlint:standard:max-line-length")
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<packages xmlns=\"https://www.cmgapps.com\" xsi:schemaLocation=\"https://www.cmgapps.com https://www.cmgapps.com/xsd/packages.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <latest>\n" +
                "    <package currentVersion=\"1.0.0\">\n" +
                "      npm-dependency\n" +
                "    </package>\n" +
                "  </latest>\n" +
                "  <outdated/>\n" +
                "</packages>\n"

        assertThat(
            reportOutputFile.toFile().readText(),
            `is`(expected),
        )
    }
}

private class TestWorkExecutor(
    project: Project,
) : WorkerExecutor {
    val workQueue =
        object : WorkQueue {
            override fun <T : WorkParameters?> submit(
                workActionClass: Class<out WorkAction<T>>?,
                parameterAction: Action<in T>?,
            ) {
                assertThat(workActionClass, `is`(CheckNpmPackageAction::class.java))

                val action = TestCheckNpmPackageAction(project)

                @Suppress("UNCHECKED_CAST")
                parameterAction?.execute(action.parameters as T)
                action.parameters.networkService.set(TestNetworkService(project))

                action.execute()
            }

            override fun await() {
            }
        }

    override fun noIsolation(): WorkQueue = workQueue

    override fun noIsolation(action: Action<in WorkerSpec>?): WorkQueue = workQueue

    override fun classLoaderIsolation(): WorkQueue {
        error("Not yet implemented")
    }

    override fun classLoaderIsolation(action: Action<in ClassLoaderWorkerSpec>?): WorkQueue {
        error("Not yet implemented")
    }

    override fun processIsolation(): WorkQueue {
        error("Not yet implemented")
    }

    override fun processIsolation(action: Action<in ProcessWorkerSpec>?): WorkQueue {
        error("Not yet implemented")
    }

    override fun await() {}
}

private class TestCheckNpmPackageAction(
    project: Project,
) : CheckNpmPackageAction() {
    val params =
        object : Params {
            override val dependencyName: Property<String> = project.objects.property()

            override val dependencyVersion: Property<String> =
                project.objects.property()

            override val outputDirectory: DirectoryProperty =
                project.objects.directoryProperty()

            override val networkService: Property<NetworkService> =
                project.objects.property<NetworkService>().value(TestNetworkService(project))
        }

    override fun getParameters(): Params = params
}

private class TestNetworkService(
    project: Project,
) : NetworkService() {
    val params =
        object : Params {
            override val baseUrl: Property<String> = project.objects.property<String>().value("https://cmgapps.com")
            override val additionalHeaders: MapProperty<String, String> = project.objects.mapProperty()
            override val engine: Property<HttpClientEngine> =
                project.objects.property<HttpClientEngine>().value(
                    MockEngine { request ->
                        when {
                            request.url.pathSegments[0] == TEST_DEP_NAME ->
                                respond(
                                    content =
                                        ByteReadChannel(
                                            Json.encodeToString(
                                                NpmResponse(
                                                    TEST_DEP_NAME,
                                                    TEST_DEP_VERSION,
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

    override fun getParameters(): Params = params
}
