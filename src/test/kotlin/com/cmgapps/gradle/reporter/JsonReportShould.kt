/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.JSON_REPORT_NAME
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
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ReportTaskExtension::class)
class JsonReportShould : OutputStreamTest() {
    @ReportTask
    lateinit var task: Task

    @Test
    fun `report outdated and latest`() {
        TestJsonReporter(
            task = task,
            outdated = outdatedPackages,
            latest = latestPackages,
        ).writePackages(outputStream)

        @Language("json")
        val expected =
            "{\n" +
                "    \"latest\": [\n" +
                "        {\n" +
                "            \"name\": \"latest list\",\n" +
                "            \"version\": \"1.0.0\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"outdated\": [\n" +
                "        {\n" +
                "            \"name\": \"outdated lib\",\n" +
                "            \"currentVersion\": \"1.0.0\",\n" +
                "            \"latestVersion\": \"2.0.0\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n"

        assertThat(
            outputStream.asString(),
            `is`(expected),
        )
    }

    @Test
    fun `report outdated only`() {
        TestJsonReporter(
            task = task,
            outdated = outdatedPackages,
            latest = emptyList(),
        ).writePackages(outputStream)

        @Language("json")
        val expected =
            "{\n" +
                "    \"latest\": [],\n" +
                "    \"outdated\": [\n" +
                "        {\n" +
                "            \"name\": \"outdated lib\",\n" +
                "            \"currentVersion\": \"1.0.0\",\n" +
                "            \"latestVersion\": \"2.0.0\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n"

        assertThat(
            outputStream.asString(),
            `is`(expected),
        )
    }

    @Test
    fun `report latest only`() {
        TestJsonReporter(
            task = task,
            outdated = emptyList(),
            latest = latestPackages,
        ).writePackages(outputStream)

        @Language("json")
        val expected =
            "{\n" +
                "    \"latest\": [\n" +
                "        {\n" +
                "            \"name\": \"latest list\",\n" +
                "            \"version\": \"1.0.0\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"outdated\": []\n" +
                "}\n"

        assertThat(
            outputStream.asString(),
            `is`(expected),
        )
    }
}

private class TestJsonReporter(
    task: Task,
    override var outdated: List<Package>,
    override var latest: List<Package>,
) : JsonReport(JSON_REPORT_NAME, task) {
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
