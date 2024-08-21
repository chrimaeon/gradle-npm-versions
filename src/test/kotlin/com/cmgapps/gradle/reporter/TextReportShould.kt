/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.PLAIN_TEXT_REPORT_NAME
import com.cmgapps.gradle.extension.ReportTask
import com.cmgapps.gradle.extension.ReportTaskExtension
import com.cmgapps.gradle.model.Package
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ReportTaskExtension::class)
class TextReportShould : OutputStreamTest() {
    @ReportTask
    lateinit var task: Task

    @Test
    fun `should print header`() {
        TestTextReport(
            task = task,
            emptyList(),
            emptyList(),
        ).writePackages(outputStream)

        assertThat(
            outputStream.asString(),
            `is`(
                "┌──────────────┐\n" +
                    "│ NPM Packages │\n" +
                    "└──────────────┘\n\n",
            ),
        )
    }

    @Test
    fun `should print outdated and latest`() {
        TestTextReport(
            task = task,
            outdated = outdatedPackages,
            latest = latestPackages,
        ).writePackages(outputStream)

        assertThat(
            outputStream.asString(),
            `is`(
                "┌──────────────┐\n" +
                    "│ NPM Packages │\n" +
                    "└──────────────┘\n\n" +
                    "The following packages are using the latest version:\n" +
                    " • latest list:1.0.0\n\n" +
                    "The following packages have updated versions:\n" +
                    " • outdated lib [1.0.0 -> 2.0.0]\n",
            ),
        )
    }

    @Test
    fun `should print outdated only`() {
        TestTextReport(
            task = task,
            outdated = outdatedPackages,
            emptyList(),
        ).writePackages(outputStream)

        assertThat(
            outputStream.asString(),
            `is`(
                "┌──────────────┐\n" +
                    "│ NPM Packages │\n" +
                    "└──────────────┘\n\n" +
                    "The following packages have updated versions:\n" +
                    " • outdated lib [1.0.0 -> 2.0.0]\n",
            ),
        )
    }

    @Test
    fun `should only print latest`() {
        TestTextReport(
            task = task,
            outdated = emptyList(),
            latest = latestPackages,
        ).writePackages(outputStream)

        assertThat(
            String(outputStream.toByteArray()),
            `is`(
                "┌──────────────┐\n" +
                    "│ NPM Packages │\n" +
                    "└──────────────┘\n\n" +
                    "The following packages are using the latest version:\n" +
                    " • latest list:1.0.0\n",
            ),
        )
    }
}

class TestTextReport(
    task: Task,
    override var outdated: List<Package>,
    override var latest: List<Package>,
) : TextReport(PLAIN_TEXT_REPORT_NAME, task) {
    override fun getRequired(): Property<Boolean> =
        ProjectBuilder
            .builder()
            .build()
            .objects
            .property()

    override fun getOutputLocation(): RegularFileProperty =
        ProjectBuilder
            .builder()
            .build()
            .objects
            .fileProperty()

    override fun getProjectLayout(): ProjectLayout = ProjectBuilder.builder().build().layout
}
