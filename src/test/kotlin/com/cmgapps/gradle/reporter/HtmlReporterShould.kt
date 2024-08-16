package com.cmgapps.gradle.reporter

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HtmlReporterShould : OutputStreamTest() {
    private lateinit var normalizeCss: String
    private lateinit var styleCss: String

    @BeforeEach
    fun setup() {
        normalizeCss = javaClass.getResourceAsStream("/normalize.css")!!.bufferedReader().use { it.readText() }
        styleCss = javaClass.getResourceAsStream("/style.css")!!.bufferedReader().use { it.readText() }
    }

    @Test
    fun `report outdated and latest`() {
        HtmlReporter(outdated = outdated, latest = latest).writePackages(outputStream)

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
        HtmlReporter(outdated = outdated, latest = emptyList()).writePackages(outputStream)

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
        HtmlReporter(outdated = emptyList(), latest = latest).writePackages(outputStream)

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
