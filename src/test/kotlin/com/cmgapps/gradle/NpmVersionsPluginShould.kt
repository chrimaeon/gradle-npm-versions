/*
 * Copyright (c) 2026. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.junit.jupiter.api.Test

class NpmVersionsPluginShould {
    @Test
    fun `register NpmVersionExtension by name`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply(NpmVersionsPlugin::class.java)

        assertThat(project.extensions.findByName("npmVersions"), `is`(notNullValue()))
    }

    @Test
    fun `register NpmVersionReportExtension by name`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply(NpmVersionsPlugin::class.java)

        assertThat(
            (project.extensions.getByType(NpmVersionsExtension::class.java) as ExtensionAware).extensions.findByName(
                "reports",
            ),
            `is`(notNullValue()),
        )
    }

    @Test
    fun `register the task`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        project.plugins.apply(NpmVersionsPlugin::class.java)

        assertThat(
            project.tasks.findByName("npmVersions"),
            `is`(notNullValue()),
        )
    }
}
