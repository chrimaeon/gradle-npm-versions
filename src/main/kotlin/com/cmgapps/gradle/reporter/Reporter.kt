/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import java.io.PrintStream

interface Reporter {
    fun write(
        outputStream: PrintStream,
        text: String,
    )
}

abstract class PackageReporter(
    protected val outdated: List<Package>,
    protected val latest: List<Package>,
) : Reporter
