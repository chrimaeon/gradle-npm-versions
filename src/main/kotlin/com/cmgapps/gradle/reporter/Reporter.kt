/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import org.gradle.api.Task
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport
import java.io.OutputStream

interface PackageReport : Report {
    var outdated: List<Package>
    var latest: List<Package>
}

abstract class PackageSingleFileReport(
    name: String,
    task: Task,
) : TaskGeneratedSingleFileReport(name, task),
    PackageReport {
    abstract fun writePackages(outputStream: OutputStream)
}
