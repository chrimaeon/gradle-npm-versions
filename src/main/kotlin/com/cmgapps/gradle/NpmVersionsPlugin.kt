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
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

@Suppress("unused")
class NpmVersionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val npmVersionsExtension = extensions.create<NpmVersionsExtension>("npmVersions")

            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                val multiplatform = target.extensions.getByType<KotlinMultiplatformExtension>()

                val serviceProvider =
                    gradle.sharedServices.registerIfAbsent("npmNetworkService", NetworkService::class.java) {
                        this.parameters.baseUrl.set("https://registry.npmjs.org/")
                        this.parameters.additionalHeaders.put(HttpHeaders.Accept, "application/vnd.npm.install-v1+json")
                    }

                val taskProvider =
                    tasks.register(
                        "npmVersions",
                        NpmVersionTask::class.java,
                    ) {
                        outputs.upToDateWhen { false }
                        group = "help"
                        outputDirectory.set(layout.buildDirectory.dir("intermediates/gradle-npm-version"))
                        networkService.set(serviceProvider)
                        usesService(serviceProvider)
                        reports.plainText.required.set(npmVersionsExtension.plainText.enabled)
                        reports.plainText.outputLocation.set(npmVersionsExtension.plainText.outputFile)
                    }

                taskProvider.configure {
                    multiplatform.targets.all {
                        if (this.platformType != KotlinPlatformType.js) {
                            return@all
                        }

                        val compilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                        val runtimeConfigurationName =
                            compilation.runtimeDependencyConfigurationName
                                ?: compilation.compileDependencyConfigurationName

                        val runtimeConfiguration = configurations.named(runtimeConfigurationName)

                        configurationToCheck(runtimeConfiguration)
                    }
                }
            }
        }
    }
}
