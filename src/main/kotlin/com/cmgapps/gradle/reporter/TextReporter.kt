/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import java.io.OutputStream
import java.io.PrintStream

class TextReporter(
    outdated: List<Package>,
    latest: List<Package>,
) : PackageReporter(outdated, latest) {
    override fun write(
        outputStream: PrintStream,
        text: String,
    ) {
        outputStream.println(text)
    }

    fun writePackages(outputStream: OutputStream) {
        PrintStream(outputStream).use { printStream ->
            write(printStream, "┌──────────────┐")
            write(printStream, "│ NPM Packages │")
            write(printStream, "└──────────────┘\n")
            if (latest.isNotEmpty()) {
                write(printStream, "The following packages are using the latest version:")
                latest.sortedBy { it.name }.forEach {
                    write(printStream, " \u2022 ${it.name}:${it.currentVersion}")
                }
            }

            if (outdated.isNotEmpty()) {
                write(printStream, "\nThe following packages have updated versions:")
                outdated.sortedBy { it.name }.forEach {
                    write(printStream, " \u2022 ${it.name} [${it.currentVersion} -> ${it.availableVersion}]")
                }
            }
        }
    }
}
