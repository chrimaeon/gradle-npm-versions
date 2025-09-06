/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.service.NetworkService
import io.ktor.http.HttpHeaders
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.reporting.SingleFileReport
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

@Suppress("unused")
class NpmVersionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val npmVersionsExtension = extensions.create("npmVersions", NpmVersionsExtension::class.java)

            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                val multiplatform = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

                val serviceProvider =
                    gradle.sharedServices.registerIfAbsent("npmNetworkService", NetworkService::class.java) {
                        it.parameters.baseUrl.set("https://registry.npmjs.org/")
                        it.parameters.additionalHeaders.put(HttpHeaders.Accept, "application/vnd.npm.install-v1+json")
                    }

                val taskProvider =
                    tasks.register(
                        "npmVersions",
                        NpmVersionTask::class.java,
                    ) {
                        it.outputs.upToDateWhen { false }
                        it.group = "help"
                        it.outputDirectory.set(layout.buildDirectory.dir("intermediates/gradle-npm-version"))
                        it.networkService.set(serviceProvider)
                        it.usesService(serviceProvider)
                        it.reports.forEach {
                            when (it.name) {
                                PLAIN_TEXT_REPORT_NAME -> it.configureReports(npmVersionsExtension.plainText)
                                JSON_REPORT_NAME -> it.configureReports(npmVersionsExtension.json)
                                HTML_REPORT_NAME -> it.configureReports(npmVersionsExtension.html)
                                XML_REPORT_NAME -> it.configureReports(npmVersionsExtension.xml)
                                else -> throw IllegalStateException("report configuration not provided")
                            }
                        }
                    }

                taskProvider.configure { task ->
                    multiplatform.targets.all { target ->
                        if (target.platformType != KotlinPlatformType.js) {
                            return@all
                        }

                        val compilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                        val runtimeConfigurationName =
                            compilation.runtimeDependencyConfigurationName
                                ?: compilation.compileDependencyConfigurationName

                        val runtimeConfiguration = configurations.named(runtimeConfigurationName)

                        task.configurationToCheck(runtimeConfiguration)
                    }
                }
            }
        }
    }

    private fun SingleFileReport.configureReports(reporter: Reporter) {
        required.set(reporter.enabled)
        outputLocation.set(reporter.outputFile)
    }
}
