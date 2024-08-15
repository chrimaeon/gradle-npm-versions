package com.cmgapps.gradle.reporter

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class JsonReporterShould : OutputStreamTest() {
    @Test
    fun `report outdated and latest`() {
        JsonReporter(outdated = outdated, latest = latest).writePackages(outputStream)

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
        JsonReporter(outdated = outdated, latest = emptyList()).writePackages(outputStream)

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
        JsonReporter(outdated = emptyList(), latest = latest).writePackages(outputStream)

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
