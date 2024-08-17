/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.util.plus
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class NpmVersionsTaskShould {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: Path

    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        buildFile = File(testProjectDir.toFile(), "build.gradle.kts")

        @Language("gradle")
        val build =
            """
            import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
            
            plugins {
                id("org.jetbrains.kotlin.multiplatform") version "2.0.0" apply false
                id("com.cmgapps.npm.versions") version "1.0.0" apply false
            }
            
            repositories {
                mavenCentral()
            }
            
            """.trimIndent()
        buildFile + build
    }

    @Disabled(
        "> Could not create task ':runNpmVersions'.\n" +
            "   > Could not create task of type 'NpmVersionTask'.\n" +
            "      > Could not generate a decorated class for type NpmVersionTask.\n" +
            "         > org/jetbrains/kotlin/gradle/targets/js/npm/NpmDependency",
    )
    @Test
    fun `run task`() {
        val taskName = "runNpmVersions"

        @Language("gradle")
        val taskConfiguration =
            """

            val configuration = configurations.create("npmVersionConfig")

            dependencies {
                add("npmVersionConfig", NpmDependency(
                    objectFactory = project.objects,
                    name = "npm-dependency",
                    version = "1.0.0")
                )
            }

            tasks.register<com.cmgapps.gradle.NpmVersionTask>("$taskName") {
                configurationToCheck(provider { configuration })
            }
            """.trimIndent()
        buildFile + (taskConfiguration)
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(taskName)
                .withDebug(true)
                .withPluginClasspath()
                .build()

        println(result.output)
        assertThat(result.task(taskName)?.outcome, `is`(TaskOutcome.SUCCESS))
    }
}
