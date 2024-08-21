/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import org.gradle.api.Task
import java.io.OutputStream
import java.io.PrintStream

abstract class TextReport(
    name: String,
    task: Task,
) : PackageSingleFileReport(name, task) {
    override fun writePackages(outputStream: OutputStream) {
        PrintStream(outputStream).use { printStream ->
            printStream.println("┌──────────────┐")
            printStream.println("│ NPM Packages │")
            printStream.println("└──────────────┘\n")
            if (latest.isNotEmpty()) {
                printStream.println("The following packages are using the latest version:")
                latest.sortedBy { it.name }.forEach {
                    printStream.println(" \u2022 ${it.name}:${it.currentVersion}")
                }
            }

            if (outdated.isNotEmpty()) {
                if (latest.isNotEmpty()) {
                    printStream.println("")
                }
                printStream.println("The following packages have updated versions:")
                outdated.sortedBy { it.name }.forEach {
                    printStream.println(" \u2022 ${it.name} [${it.currentVersion} -> ${it.availableVersion}]")
                }
            }
        }
    }
}
