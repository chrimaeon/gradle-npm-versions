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
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.withType

class TestConvention : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.withType<Test> {
                testLogging {
                    events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
                }
                afterSuite(
                    KotlinClosure2<TestDescriptor, TestResult, Unit>(
                        owner = this,
                        thisObject = this,
                        function = { desc, result ->
                            if (desc.parent == null) {
                                logger.logResults(result)
                            }
                        },
                    ),
                )
            }
        }
    }
}

const val CSI = "\u001B["
const val ANSI_RED = "31"
const val ANSI_GREEN = "32"
const val ANSI_YELLOW = "33"
const val ANSI_BOLD = "1"

private fun Logger.logResults(result: TestResult) {
    val message = "{}; {}; {}\n"

    val params =
        buildList {
            add("${result.successfulTestCount} ${getFormattedResult(ResultType.SUCCESS)}")
            add("${result.skippedTestCount} ${getFormattedResult(ResultType.SKIPPED)}")
            add("${result.failedTestCount} ${getFormattedResult(ResultType.FAILURE)}")
        }.toTypedArray()

    this.lifecycle(message, *params)
}

private fun getFormattedResult(result: ResultType): String =
    buildString {
        val isAnsiColorTerm = System.getenv("TERM")?.lowercase()?.contains("color") ?: false
        val (color, text) =
            when (result) {
                ResultType.SUCCESS -> ANSI_GREEN to "PASSED"
                ResultType.FAILURE -> ANSI_RED to "FAILED"
                ResultType.SKIPPED -> ANSI_YELLOW to "SKIPPED"
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
