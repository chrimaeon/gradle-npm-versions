package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class TextReporterShould : OutputStreamTest() {
    @Test
    fun `should print header`() {
        TextReporter(emptyList(), emptyList()).writePackages(outputStream)

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
        TextReporter(
            outdated = outdated,
            latest = latest,
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
        TextReporter(
            outdated = outdated,
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
        TextReporter(
            outdated =
                emptyList(),
            listOf(
                Package(
                    name = "latest list",
                    currentVersion = "1.0.0",
                    availableVersion = "1.0.0",
                ),
            ),
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
