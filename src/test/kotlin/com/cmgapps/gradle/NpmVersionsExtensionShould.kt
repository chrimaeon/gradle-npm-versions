package com.cmgapps.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NpmVersionsExtensionShould {
    lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterEnabledStateConvention")
    fun `set enabled state via convention`(
        name: String,
        reporter: Reporter,
        enabled: Boolean,
    ) {
        assertThat(reporter.enabled.get(), `is`(enabled))
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterOutputFileConvention")
    fun `set output file via convention`(
        name: String,
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
        name: String,
        reporter: Reporter,
        enabled: Boolean,
    ) {
        reporter.enabled.set(enabled)
        assertThat(reporter.enabled.get(), `is`(enabled))
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterEnabledStateAction")
    fun `configure enabled state via action`(
        name: String,
        reporter: Reporter,
        action: Action<in Reporter>,
        enabled: Boolean,
    ) {
        action.execute(reporter)

        assertThat(reporter.enabled.get(), `is`(enabled))
    }

    @ParameterizedTest(name = "for reporter {0}")
    @MethodSource("provideReporterOutputFile")
    fun `configure output file`(
        name: String,
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
        name: String,
        reporter: Reporter,
        action: Action<in Reporter>,
        file: File,
    ) {
        action.execute(reporter)

        assertThat(
            reporter.outputFile
                .get()
                .asFile.path,
            endsWith(file.path),
        )
    }

    companion object {
        private val extension: NpmVersionsExtension
            get() {
                val project = ProjectBuilder.builder().build()
                return object : NpmVersionsExtension(project, project.objects) {}
            }

        @JvmStatic
        fun provideReporterEnabledStateConvention(): Stream<Arguments> =
            Stream.of(
                Arguments.of(PLAIN_TEXT_REPORT_NAME, extension.plainText, true),
                Arguments.of(HTML_REPORT_NAME, extension.html, false),
                Arguments.of(JSON_REPORT_NAME, extension.json, false),
                Arguments.of(XML_REPORT_NAME, extension.xml, false),
            )

        @JvmStatic
        fun provideReporterEnabledState(): Stream<Arguments> =
            Stream.of(
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

        @JvmStatic
        fun provideReporterEnabledStateAction(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    PLAIN_TEXT_REPORT_NAME,
                    extension.plainText,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.enabled.set(false)
                        }
                    },
                    false,
                ),
                Arguments.of(
                    HTML_REPORT_NAME,
                    extension.html,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.enabled.set(true)
                        }
                    },
                    true,
                ),
                Arguments.of(
                    JSON_REPORT_NAME,
                    extension.json,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.enabled.set(true)
                        }
                    },
                    true,
                ),
                Arguments.of(
                    XML_REPORT_NAME,
                    extension.xml,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.enabled.set(true)
                        }
                    },
                    true,
                ),
            )

        @JvmStatic
        fun provideReporterOutputFileConvention(): Stream<Arguments> =
            Stream.of(
                Arguments.of(PLAIN_TEXT_REPORT_NAME, extension.plainText, "reports/npmVersions/report.txt"),
                Arguments.of(HTML_REPORT_NAME, extension.html, "reports/npmVersions/report.html"),
                Arguments.of(JSON_REPORT_NAME, extension.json, "reports/npmVersions/report.json"),
                Arguments.of(XML_REPORT_NAME, extension.xml, "reports/npmVersions/report.xml"),
            )

        @JvmStatic
        fun provideReporterOutputFile(): Stream<Arguments> =
            Stream.of(
                Arguments.of(PLAIN_TEXT_REPORT_NAME, extension.plainText, File("foo/bar.txt")),
                Arguments.of(HTML_REPORT_NAME, extension.html, File("foo/bar.html")),
                Arguments.of(JSON_REPORT_NAME, extension.json, File("foo/bar.json")),
                Arguments.of(XML_REPORT_NAME, extension.xml, File("foo/bar.xml")),
            )

        @JvmStatic
        fun provideReporterOutputFileAction(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    PLAIN_TEXT_REPORT_NAME,
                    extension.plainText,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.outputFile.set(File("foo/bar.txt"))
                        }
                    },
                    File("foo/bar.txt"),
                ),
                Arguments.of(
                    HTML_REPORT_NAME,
                    extension.html,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.outputFile.set(File("foo/bar.html"))
                        }
                    },
                    File("foo/bar.html"),
                ),
                Arguments.of(
                    JSON_REPORT_NAME,
                    extension.json,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.outputFile.set(File("foo/bar.json"))
                        }
                    },
                    File("foo/bar.json"),
                ),
                Arguments.of(
                    XML_REPORT_NAME,
                    extension.xml,
                    object : Action<Reporter> {
                        override fun execute(reporter: Reporter) {
                            reporter.outputFile.set(File("foo/bar.xml"))
                        }
                    },
                    File("foo/bar.xml"),
                ),
            )
    }
}
