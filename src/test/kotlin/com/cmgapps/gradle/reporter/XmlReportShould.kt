/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.XML_REPORT_NAME
import com.cmgapps.gradle.extension.ReportTask
import com.cmgapps.gradle.extension.ReportTaskExtension
import com.cmgapps.gradle.matcher.DoesNotThrowExceptionMatcher.Companion.doesNotThrowException
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
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

@ExtendWith(ReportTaskExtension::class)
class XmlReportShould : OutputStreamTest() {
    @ReportTask
    lateinit var task: Task

    @Test
    fun `report outdated and latest`() {
        TestXmlReport(
            task = task,
            outdated = outdatedPackages,
            latest = latestPackages,
        ).writePackages(outputStream)

        @Suppress("ktlint:standard:max-line-length")
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<packages xmlns=\"https://www.cmgapps.com\" xsi:schemaLocation=\"https://www.cmgapps.com https://www.cmgapps.com/xsd/packages.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <latest>\n" +
                "    <package currentVersion=\"1.0.0\">\n" +
                "      latest list\n" +
                "    </package>\n" +
                "  </latest>\n" +
                "  <outdated>\n" +
                "    <package latestVersion=\"2.0.0\" currentVersion=\"1.0.0\">\n" +
                "      outdated lib\n" +
                "    </package>\n" +
                "  </outdated>\n" +
                "</packages>\n"

        assertThat(outputStream.asString(), `is`(expected))
    }

    @Test
    fun `report outdated only`() {
        TestXmlReport(
            task = task,
            outdated = outdatedPackages,
            latest = emptyList(),
        ).writePackages(outputStream)

        @Suppress("ktlint:standard:max-line-length")
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<packages xmlns=\"https://www.cmgapps.com\" xsi:schemaLocation=\"https://www.cmgapps.com https://www.cmgapps.com/xsd/packages.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <latest/>\n" +
                "  <outdated>\n" +
                "    <package latestVersion=\"2.0.0\" currentVersion=\"1.0.0\">\n" +
                "      outdated lib\n" +
                "    </package>\n" +
                "  </outdated>\n" +
                "</packages>\n"

        assertThat(outputStream.asString(), `is`(expected))
    }

    @Test
    fun `report latest only`() {
        TestXmlReport(
            task = task,
            outdated = emptyList(),
            latest = latestPackages,
        ).writePackages(outputStream)

        @Suppress("ktlint:standard:max-line-length")
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<packages xmlns=\"https://www.cmgapps.com\" xsi:schemaLocation=\"https://www.cmgapps.com https://www.cmgapps.com/xsd/packages.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <latest>\n" +
                "    <package currentVersion=\"1.0.0\">\n" +
                "      latest list\n" +
                "    </package>\n" +
                "  </latest>\n" +
                "  <outdated/>\n" +
                "</packages>\n"

        assertThat(outputStream.asString(), `is`(expected))
    }

    @Test
    fun `create valid xml`() {
        TestXmlReport(
            task = task,
            outdated = outdatedPackages,
            latest = latestPackages,
        ).writePackages(outputStream)

        val schema =
            SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(StreamSource(javaClass.getResourceAsStream("/xsd/packages.xsd")))

        assertThat({
            schema.newValidator().validate(StreamSource(ByteArrayInputStream(outputStream.toByteArray())))
        }, doesNotThrowException())
    }
}

private class TestXmlReport(
    task: Task,
    override var outdated: List<Package>,
    override var latest: List<Package>,
) : XmlReport(XML_REPORT_NAME, task) {
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
