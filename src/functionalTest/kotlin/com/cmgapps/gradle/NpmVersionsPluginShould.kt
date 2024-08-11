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

class NpmVersionsPluginShould {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: Path

    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        File(testProjectDir.toFile(), "settings.gradle.kts").writeText("rootProject.name = \"gradle-npm-plugin\"")
        buildFile = File(testProjectDir.toFile(), "build.gradle.kts")

        @Language("gradle")
        val build =
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform") version "1.9.23"
                id("com.cmgapps.npm.versions") version "1.0.0"
            }
            
            repositories {
                mavenCentral()
            }
            
            """.trimIndent()
        buildFile + build
    }

    @Disabled("KotlinMultiplatformExtension not present")
    @Test
    fun `report npm packages`() {
        @Language("gradle")
        val kotlinExtension =
            """
            kotlin {
            
                jvm()
                js(IR) {
                    browser()
                }
                
                sourceSets {
                    named("jsMain") {
                        dependencies {
                            implementation("org.apache.commons:commons-csv:1.9.0")
                            implementation(npm("bootstrap", "5.3.3"))
                            implementation(npm("kotlin", "1.0"))
                        }
                    }
                }
            }
            """.trimIndent()

        buildFile + kotlinExtension

        val task = ":npmVersions"

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(task)
                .withPluginClasspath()
                .build()
        println(result.output)
        assertThat(result.task(task)?.outcome, `is`(TaskOutcome.FAILED))
    }
}
