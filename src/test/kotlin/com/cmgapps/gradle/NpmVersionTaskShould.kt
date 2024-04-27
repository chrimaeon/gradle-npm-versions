/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class NpmVersionTaskShould {
    @TempDir
    lateinit var testProjectDir: Path

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project =
            ProjectBuilder.builder()
                .withProjectDir(testProjectDir.toFile())
                .build()

        project.plugins.apply(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin::class.java)
    }

    @Test
    @Disabled("KotlinMultiplatformPlugin cannot be applied")
    fun `setup correctly`() {
        val outputDir = testProjectDir.resolve("output")
        val task =
            project.tasks.create(
                "npmVersions",
                NpmVersionTask::class.java,
            )

        val configuration = project.configurations.create("implementation")

        configuration.dependencies.add(
            NpmDependency(
                objectFactory = project.objects,
                name = "npm-dependency",
                version = "1.0.0",
            ),
        )

        task.outputDirectory.set(outputDir.toFile())
        task.configurationToCheck(
            project.provider {
                configuration
            },
        )

        task.action()

        assertThat(outputDir.toFile().exists(), `is`(true))
    }
}
