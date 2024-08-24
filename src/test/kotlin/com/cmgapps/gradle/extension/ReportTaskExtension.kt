/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.extension

import org.gradle.api.DefaultTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ReportTask

class ReportTaskExtension : BeforeEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        val reportTask =
            with(ProjectBuilder.builder().build()) {
                tasks.create("report", DefaultTask::class.java)
            }

        context.requiredTestInstances.allInstances.forEach { testInstance ->
            testInstance.javaClass.declaredFields.forEach { field ->
                if (field.annotations.filterIsInstance<ReportTask>().isNotEmpty()) {
                    field.set(testInstance, reportTask)
                }
            }
        }
    }
}
