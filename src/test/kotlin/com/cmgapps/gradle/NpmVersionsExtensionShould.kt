package com.cmgapps.gradle

import org.gradle.api.Action
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class NpmVersionsExtensionShould {
    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterEnabledStateConvention")
    fun `set enabled state via convention`(
        @Suppress("UNUSED_PARAMETER") name: String,
        reporter: Reporter,
        enabled: Boolean,
    ) {
        assertThat(reporter.enabled.get(), `is`(enabled))
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterOutputFileConvention")
    fun `set output file via convention`(
        @Suppress("UNUSED_PARAMETER") name: String,
        reporter: Reporter,
        path: String,
    ) {
        assertThat(
            reporter.outputFile
                .get()
                .asFile.path,
            endsWith(path),
        )
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterEnabledState")
    fun `configure enabled state`(
        @Suppress("UNUSED_PARAMETER") name: String,
        reporter: Reporter,
        enabled: Boolean,
    ) {
        reporter.enabled.set(enabled)
        assertThat(reporter.enabled.get(), `is`(enabled))
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterEnabledStateAction")
    fun `configure enabled state via action`(
        @Suppress("UNUSED_PARAMETER") name: String,
        func: (Action<in Reporter>) -> Unit,
        reporter: Reporter,
        enabled: Boolean,
    ) {
        func { reporter ->
            reporter.enabled.set(enabled)
        }

        assertThat(reporter.enabled.get(), `is`(enabled))
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterOutputFile")
    fun `configure output file`(
        @Suppress("UNUSED_PARAMETER") name: String,
        reporter: Reporter,
        file: File,
    ) {
        reporter.outputFile.set(file)

        assertThat(
            reporter.outputFile
                .get()
                .asFile.path,
            endsWith(file.path),
        )
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterOutputFileAction")
    fun `configure output file via action`(
        @Suppress("UNUSED_PARAMETER") name: String,
        func: (Action<in Reporter>) -> Unit,
        reporter: Reporter,
        file: File,
    ) {
        func { reporter ->
            reporter.outputFile.set(file)
        }

        assertThat(
            reporter.outputFile
                .get()
                .asFile.path,
            endsWith(file.path),
        )
    }

    companion object {
        @JvmStatic
        fun provideReporterEnabledStateConvention(): Stream<Arguments> {
            val extension =
                ProjectBuilder.builder().build().run {
                    object : NpmVersionsExtension(this, this.objects) {}
                }

            return Stream.of(
                Arguments.of(PLAIN_TEXT_REPORT_NAME, extension.plainText, true),
                Arguments.of(HTML_REPORT_NAME, extension.html, false),
                Arguments.of(JSON_REPORT_NAME, extension.json, false),
                Arguments.of(XML_REPORT_NAME, extension.xml, false),
            )
        }

        @JvmStatic
        fun provideReporterEnabledState(): Stream<Arguments> {
            val extension =
                ProjectBuilder.builder().build().run {
                    object : NpmVersionsExtension(this, this.objects) {}
                }

            return Stream.of(
                Arguments.of(
                    PLAIN_TEXT_REPORT_NAME,
                    extension.plainText,
                    false,
                ),
                Arguments.of(
                    HTML_REPORT_NAME,
                    extension.html,
                    true,
                ),
                Arguments.of(
                    JSON_REPORT_NAME,
                    extension.json,
                    true,
                ),
                Arguments.of(
                    XML_REPORT_NAME,
                    extension.xml,
                    true,
                ),
            )
        }

        @JvmStatic
        fun provideReporterEnabledStateAction(): Stream<Arguments> {
            val extension =
                ProjectBuilder.builder().build().run {
                    object : NpmVersionsExtension(this, this.objects) {}
                }

            val plainTextAction: (Action<in Reporter>) -> Unit = extension::plainText
            val htmlAction: (Action<in Reporter>) -> Unit = extension::html
            val jsonAction: (Action<in Reporter>) -> Unit = extension::json
            val xmlAction: (Action<in Reporter>) -> Unit = extension::xml

            return Stream.of(
                Arguments.of(
                    PLAIN_TEXT_REPORT_NAME,
                    plainTextAction,
                    extension.plainText,
                    false,
                ),
                Arguments.of(
                    HTML_REPORT_NAME,
                    htmlAction,
                    extension.html,
                    true,
                ),
                Arguments.of(
                    JSON_REPORT_NAME,
                    jsonAction,
                    extension.json,
                    true,
                ),
                Arguments.of(
                    XML_REPORT_NAME,
                    xmlAction,
                    extension.xml,
                    true,
                ),
            )
        }

        @JvmStatic
        fun provideReporterOutputFileConvention(): Stream<Arguments> {
            val extension =
                ProjectBuilder.builder().build().run {
                    object : NpmVersionsExtension(this, this.objects) {}
                }

            return Stream.of(
                Arguments.of(PLAIN_TEXT_REPORT_NAME, extension.plainText, "reports/npmVersions/report.txt"),
                Arguments.of(HTML_REPORT_NAME, extension.html, "reports/npmVersions/report.html"),
                Arguments.of(JSON_REPORT_NAME, extension.json, "reports/npmVersions/report.json"),
                Arguments.of(XML_REPORT_NAME, extension.xml, "reports/npmVersions/report.xml"),
            )
        }

        @JvmStatic
        fun provideReporterOutputFile(): Stream<Arguments> {
            val extension =
                ProjectBuilder.builder().build().run {
                    object : NpmVersionsExtension(this, this.objects) {}
                }

            return Stream.of(
                Arguments.of(PLAIN_TEXT_REPORT_NAME, extension.plainText, File("foo/bar.txt")),
                Arguments.of(HTML_REPORT_NAME, extension.html, File("foo/bar.html")),
                Arguments.of(JSON_REPORT_NAME, extension.json, File("foo/bar.json")),
                Arguments.of(XML_REPORT_NAME, extension.xml, File("foo/bar.xml")),
            )
        }

        @JvmStatic
        fun provideReporterOutputFileAction(): Stream<Arguments> {
            val extension =
                ProjectBuilder.builder().build().run {
                    object : NpmVersionsExtension(this, this.objects) {}
                }

            val plainTextAction: (Action<in Reporter>) -> Unit = extension::plainText
            val htmlAction: (Action<in Reporter>) -> Unit = extension::html
            val jsonAction: (Action<in Reporter>) -> Unit = extension::json
            val xmlAction: (Action<in Reporter>) -> Unit = extension::xml

            return Stream.of(
                Arguments.of(
                    PLAIN_TEXT_REPORT_NAME,
                    plainTextAction,
                    extension.plainText,
                    File("foo/bar.txt"),
                ),
                Arguments.of(
                    HTML_REPORT_NAME,
                    htmlAction,
                    extension.html,
                    File("foo/bar.html"),
                ),
                Arguments.of(
                    JSON_REPORT_NAME,
                    jsonAction,
                    extension.json,
                    File("foo/bar.json"),
                ),
                Arguments.of(
                    XML_REPORT_NAME,
                    xmlAction,
                    extension.xml,
                    File("foo/bar.xml"),
                ),
            )
        }
    }
}
