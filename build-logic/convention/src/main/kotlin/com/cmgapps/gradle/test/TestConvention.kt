/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.test

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType

private const val CSI = "\u001B["
private const val ANSI_RED = "31"
private const val ANSI_GREEN = "32"
private const val ANSI_YELLOW = "33"
private const val ANSI_BOLD = "1"

class TestConvention : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.withType(Test::class.java).configureEach {
            addTestListener(
                object : TestListener {
                    override fun afterTest(
                        testDescriptor: TestDescriptor,
                        result: TestResult,
                    ) {
                        logger.logResults(testDescriptor, result)
                    }

                    override fun beforeSuite(suite: TestDescriptor) {
                        // NO-OP
                    }

                    override fun afterSuite(
                        suite: TestDescriptor,
                        result: TestResult,
                    ) {
                        // NO-OP
                    }

                    override fun beforeTest(testDescriptor: TestDescriptor) {
                        // NO-OP
                    }
                },
            )
        }
    }
}

private fun Logger.logResults(
    desc: TestDescriptor,
    result: TestResult,
) {
    val message = "{} > {} {}" + if (result.exception != null) "\n>\t{}\n" else "\n"

    val params =
        buildList {
            add(desc.className?.substringAfterLast('.') ?: "")
            add(desc.displayName)
            add(getFormattedResult(result))
            result.exception?.let {
                add(it.message?.replace("\n", "\n>\t") ?: "")
            }
        }.toTypedArray()

    if (result.resultType == ResultType.FAILURE) {
        this.error(message, *params)
    } else {
        this.lifecycle(message, *params)
    }
}

private fun getFormattedResult(result: TestResult): String =
    buildString {
        val isAnsiColorTerm = System.getenv("TERM")?.lowercase()?.contains("color") ?: false
        val (color, text) =
            when (result.resultType) {
                ResultType.SUCCESS -> ANSI_GREEN to "PASSED"
                ResultType.FAILURE -> ANSI_RED to "FAILED"
                ResultType.SKIPPED -> ANSI_YELLOW to "SKIPPED"
                null -> ANSI_YELLOW to "NO RESULT"
            }
        if (isAnsiColorTerm) {
            append(CSI)
            append(color)
            append(";${ANSI_BOLD}m")
        }
        append(text)

        if (isAnsiColorTerm) {
            append(CSI)
            append("0m")
        }
    }
