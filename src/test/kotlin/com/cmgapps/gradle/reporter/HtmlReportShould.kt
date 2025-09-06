/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.HTML_REPORT_NAME
import com.cmgapps.gradle.model.Package
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HtmlReportShould : OutputStreamTest() {
    private lateinit var normalizeCss: String
    private lateinit var styleCss: String

    @BeforeEach
    fun setup() {
        normalizeCss = javaClass.getResourceAsStream("/normalize.css")!!.bufferedReader().use { it.readText() }
        styleCss = javaClass.getResourceAsStream("/style.css")!!.bufferedReader().use { it.readText() }
    }

    @Test
    fun `report outdated and latest`() {
        TestHtmlReport(
            outdated = outdatedPackages,
            latest = latestPackages,
        ).writePackages(outputStream)

        @Suppress("ktlint:standard:max-line-length")
        @Language("html")
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
                "          latest list\n" +
                "        </td>\n" +
                "        <td>\n" +
                "          1.0.0\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "    <p>\n" +
                "      The following packages have updated versions\n" +
                "    </p>\n" +
                "    <table>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          outdated lib\n" +
                "        </td>\n" +
                "        <td>\n" +
                "          1.0.0 &rarr; 2.0.0\n" +
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
            outputStream.asString(),
            `is`(expected),
        )
    }

    @Test
    fun `report outdated`() {
        TestHtmlReport(
            outdated = outdatedPackages,
            latest = emptyList(),
        ).writePackages(outputStream)

        @Suppress("ktlint:standard:max-line-length")
        @Language("html")
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
                "      The following packages have updated versions\n" +
                "    </p>\n" +
                "    <table>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          outdated lib\n" +
                "        </td>\n" +
                "        <td>\n" +
                "          1.0.0 &rarr; 2.0.0\n" +
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
            outputStream.asString(),
            `is`(expected),
        )
    }

    @Test
    fun `report latest`() {
        TestHtmlReport(
            outdated = emptyList(),
            latest = latestPackages,
        ).writePackages(outputStream)

        @Suppress("ktlint:standard:max-line-length")
        @Language("html")
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
                "          latest list\n" +
                "        </td>\n" +
                "        <td>\n" +
                "          1.0.0\n" +
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
            outputStream.asString(),
            `is`(expected),
        )
    }
}

private class TestHtmlReport(
    override var outdated: List<Package>,
    override var latest: List<Package>,
) : HtmlReport(HTML_REPORT_NAME) {
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
