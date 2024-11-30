/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import groovy.lang.Closure
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.util.internal.ConfigureUtil
import java.io.File
import java.io.OutputStream

interface PackageReport {
    var outdated: List<Package>
    var latest: List<Package>
}

abstract class PackageSingleFileReport(
    private val name: String,
) : PackageReport,
    SingleFileReport {
    init {
        required.convention(false)
    }

    abstract fun writePackages(outputStream: OutputStream)

    override fun getName(): String = name

    override fun getDisplayName(): String = "NPM Versions Report for $name"

    @Deprecated("Deprecated in Java", replaceWith = ReplaceWith("getOutputLocation().set"))
    override fun setDestination(file: File) {
        outputLocation.fileValue(file)
    }

    override fun getOutputType(): Report.OutputType = Report.OutputType.FILE

    override fun configure(configure: Closure<*>): Report =
        ConfigureUtil.configureSelf(
            configure,
            this,
        )
}
