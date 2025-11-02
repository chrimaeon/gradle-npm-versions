/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.JSON_REPORT_NAME
import com.cmgapps.gradle.model.Package
import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class JsonReportShould : OutputStreamTest() {
    @Test
    fun `report outdated and latest`() {
        TestJsonReporter(
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

    @Test
    fun `create valid json`() {
        val schema =
            SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(
                    javaClass.getResourceAsStream("/schema/packages-schema.json"),
                )

        TestJsonReporter(
            outdated = outdatedPackages,
            latest = latestPackages,
        ).writePackages(outputStream)

        val result =
            schema.validate(
                outputStream.asString(),
                InputFormat.JSON,
            ) { executionContext ->
                executionContext.executionConfig({ executionConfigBuilder ->
                    executionConfigBuilder.formatAssertionsEnabled(true)
                })
            }

        assertThat(result, empty())
    }
}

private class TestJsonReporter(
    override var outdated: List<Package>,
    override var latest: List<Package>,
) : JsonReport(JSON_REPORT_NAME) {
    override fun getRequired(): Property<Boolean> =
        ProjectBuilder
            .builder()
            .build()
            .objects
            .property(Boolean::class.java)

    override fun getOutputLocation(): RegularFileProperty =
        ProjectBuilder
            .builder()
            .build()
            .objects
            .fileProperty()
}
