/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.model.NpmResponse
import com.cmgapps.gradle.model.Package
import com.cmgapps.gradle.service.NetworkService
import io.ktor.client.call.body
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.maven.artifact.versioning.ComparableVersion
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import java.io.FileOutputStream
import javax.inject.Inject

abstract class NpmVersionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
        objects: ObjectFactory,
    ) : DefaultTask() {
        private val dependenciesSetProviders: MutableList<Provider<DependencySet>> = mutableListOf()

        @get:Internal
        abstract val networkService: Property<NetworkService>

        @get:OutputDirectory
        val outputDirectory: DirectoryProperty = objects.directoryProperty()

        fun configurationToCheck(configuration: Provider<Configuration>) {
            dependenciesSetProviders.add(configuration.map { it.allDependencies })
        }

        @OptIn(ExperimentalSerializationApi::class)
        @TaskAction
        fun action() {
            val workQueue = workerExecutor.noIsolation()

            dependenciesSetProviders
                .fold(mutableListOf<NpmDependency>()) { acc, provider ->
                    acc.apply {
                        addAll(provider.get().filterIsInstance<NpmDependency>())
                    }
                }.forEach {
                    workQueue.enqueue(it)
                }

            workQueue.await()

            val outputDirectoryFile = outputDirectory.get().asFile

            val (upgradable, latest) =
                outputDirectoryFile
                    .walkTopDown()
                    .filter { it.isFile }
                    .map { Json.decodeFromStream<Package>(it.inputStream()) }
                    .partition { ComparableVersion(it.currentVersion) < ComparableVersion(it.availableVersion) }

            logger.lifecycle("┌──────────────┐")
            logger.lifecycle("│ NPM Packages │")
            logger.lifecycle("└──────────────┘\n")
            if (latest.isNotEmpty()) {
                logger.lifecycle("The following packages are using the latest version:")
                latest.sortedBy { it.name }.forEach {
                    logger.lifecycle(" · {}:{}", it.name, it.currentVersion)
                }
            }

            if (upgradable.isNotEmpty()) {
                logger.lifecycle("\nThe following packages have updated versions:")
                upgradable.sortedBy { it.name }.forEach {
                    logger.lifecycle(" · {} [{} -> {}]", it.name, it.currentVersion, it.availableVersion)
                }
            }
        }

        private fun WorkQueue.enqueue(dependency: NpmDependency) {
            submit(CheckNpmPackageAction::class.java) {
                dependencyName.set(dependency.name)
                dependencyVersion.set(dependency.version)
                outputDirectory.set(this@NpmVersionTask.outputDirectory)
                networkService.set(this@NpmVersionTask.networkService)
            }
        }
    }

abstract class CheckNpmPackageAction : WorkAction<CheckNpmPackageAction.Params> {
    override fun execute() {
        val outputDirectory = parameters.outputDirectory
        val dependencyName = parameters.dependencyName.get()
        val response =
            runBlocking {
                getLatestVersion(dependencyName)
            }

        FileOutputStream(
            outputDirectory.asFile.get().resolve(dependencyName.encodeURLPathPart() + ".json"),
        ).bufferedWriter().use {
            it.write(
                Json.encodeToString(
                    Package(
                        name = response.name,
                        currentVersion = parameters.dependencyVersion.get(),
                        availableVersion = response.version,
                    ),
                ),
            )
        }
    }

    private suspend fun getLatestVersion(packageName: String): NpmResponse =
        parameters.networkService
            .get()
            .get(packageName, "latest")
            .body()

    interface Params : WorkParameters {
        val dependencyName: Property<String>
        val dependencyVersion: Property<String>
        val outputDirectory: DirectoryProperty
        val networkService: Property<NetworkService>
    }
}
