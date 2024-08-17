/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.dsl.Tag
import com.cmgapps.gradle.dsl.TagWithText
import java.io.OutputStream
import java.io.PrintStream
import com.cmgapps.gradle.model.Package as PackageModel

internal class XmlReporter(
    outdated: List<PackageModel>,
    latest: List<PackageModel>,
) : PackageReporter(outdated = outdated, latest = latest) {
    override fun write(
        outputStream: PrintStream,
        text: String,
    ) {
        outputStream.print(text)
    }

    override fun writePackages(outputStream: OutputStream) {
        val xml =
            packages {
                latest {
                    latest.forEach {
                        `package`(currentVersion = it.currentVersion) {
                            +it.name
                        }
                    }
                }
                outdated {
                    outdated.forEach {
                        `package`(currentVersion = it.currentVersion, latestVersion = it.availableVersion) {
                            +it.name
                        }
                    }
                }
            }
        PrintStream(outputStream).use {
            write(it, xml.toString())
        }
    }
}

private class Packages : Tag("packages") {
    fun latest(init: LatestPackage.() -> Unit) = initTag(LatestPackage(), init)

    fun outdated(init: OutdatedPackage.() -> Unit) = initTag(OutdatedPackage(), init)

    override fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    ) {
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        super.render(builder, intent, format)
    }
}

private class OutdatedPackage : Tag("outdated") {
    fun `package`(
        currentVersion: String,
        latestVersion: String,
        init: Package.() -> Unit,
    ) {
        initTag(Package(), init).apply {
            this.currentVersion = currentVersion
            this.latestVersion = latestVersion
        }
    }
}

private class LatestPackage : Tag("latest") {
    fun `package`(
        currentVersion: String,
        init: Package.() -> Unit,
    ) {
        initTag(Package(), init).apply {
            this.currentVersion = currentVersion
        }
    }
}

private class Package : TagWithText("package") {
    var currentVersion: String
        get() = attributes["currentVersion"]!!
        set(value) {
            attributes["currentVersion"] = value
        }

    var latestVersion: String
        get() = attributes["latestVersion"] ?: ""
        set(value) {
            attributes["latestVersion"] = value
        }
}

private fun packages(init: Packages.() -> Unit): Packages =
    Packages().apply {
        attributes["xmlns"] = "https://www.cmgapps.com"
        attributes["xmlns:xsi"] = "http://www.w3.org/2001/XMLSchema-instance"
        attributes["xsi:schemaLocation"] = "https://www.cmgapps.com https://www.cmgapps.com/xsd/packages.xsd"
        init()
    }
