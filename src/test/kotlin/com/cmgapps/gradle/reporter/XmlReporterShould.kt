/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.matcher.DoesNotThrowExceptionMatcher.Companion.doesNotThrowException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class XmlReporterShould : OutputStreamTest() {
    @Test
    fun `report outdated and latest`() {
        XmlReporter(outdated = outdated, latest = latest).writePackages(outputStream)

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
        XmlReporter(outdated = outdated, latest = emptyList()).writePackages(outputStream)

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
        XmlReporter(outdated = emptyList(), latest = latest).writePackages(outputStream)

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
                "  </outdated>\n" +
                "</packages>\n"

        assertThat(outputStream.asString(), `is`(expected))
    }

    @Test
    fun `create valid xml`() {
        XmlReporter(outdated = outdated, latest = latest).writePackages(outputStream)

        val schema =
            SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(StreamSource(javaClass.getResourceAsStream("/xsd/packages.xsd")))

        assertThat({
            schema.newValidator().validate(StreamSource(ByteArrayInputStream(outputStream.toByteArray())))
        }, doesNotThrowException())
    }
}
