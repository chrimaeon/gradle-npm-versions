/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.model.NpmResponse
import com.cmgapps.gradle.model.Package
import com.cmgapps.gradle.reporter.HtmlReporter
import com.cmgapps.gradle.reporter.JsonReporter
import com.cmgapps.gradle.reporter.PackageReporter
import com.cmgapps.gradle.reporter.TextReporter
import com.cmgapps.gradle.service.NetworkService
import groovy.lang.Closure
import io.ktor.client.call.body
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.maven.artifact.versioning.ComparableVersion
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.reporting.Reporting
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport
import org.gradle.api.reporting.internal.TaskReportContainer
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

interface ReporterContainer : ReportContainer<Report> {
    @get:Internal
    val plainText: SingleFileReport

    @get:Internal
    val json: SingleFileReport

    @get:Internal
    val html: SingleFileReport
}

internal const val PLAIN_TEXT_REPORT_NAME = "plainText"
internal const val JSON_REPORT_NAME = "json"
internal const val HTML_REPORT_NAME = "html"

class ReporterContainerImpl(
    task: Task,
    collectionCallbackActionDecorator: CollectionCallbackActionDecorator,
) : TaskReportContainer<Report>(Report::class.java, task, collectionCallbackActionDecorator),
    ReporterContainer {
    init {
        add(TaskGeneratedSingleFileReport::class.java, PLAIN_TEXT_REPORT_NAME, task)
        add(TaskGeneratedSingleFileReport::class.java, JSON_REPORT_NAME, task)
        add(TaskGeneratedSingleFileReport::class.java, HTML_REPORT_NAME, task)
    }

    override val plainText: SingleFileReport
        get() = getByName(PLAIN_TEXT_REPORT_NAME) as SingleFileReport

    override val json: SingleFileReport
        get() = getByName(JSON_REPORT_NAME) as SingleFileReport

    override val html: SingleFileReport
        get() = getByName(HTML_REPORT_NAME) as SingleFileReport
}

abstract class NpmVersionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
        objects: ObjectFactory,
    ) : DefaultTask(),
        Reporting<ReporterContainer> {
        private val dependenciesSetProviders: MutableList<Provider<DependencySet>> = mutableListOf()

        @get:Internal
        val networkService: Property<NetworkService> = objects.property()

        @get:OutputDirectory
        val outputDirectory: DirectoryProperty =
            objects.directoryProperty().convention(project.layout.buildDirectory.dir("npm-versions/dependencies"))

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

            val (outdated, latest) =
                outputDirectoryFile
                    .walkTopDown()
                    .filter { it.isFile }
                    .map { Json.decodeFromStream<Package>(it.inputStream()) }
                    .partition { ComparableVersion(it.currentVersion) < ComparableVersion(it.availableVersion) }

            val textReporter = TextReporter(outdated = outdated, latest = latest)

            textReporter.writePackages(System.out)

            reports.filter { it.required.get() }.forEach { report ->
                val location = report.outputLocation.get().asFile
                when (report.name) {
                    PLAIN_TEXT_REPORT_NAME ->
                        textReporter.writeTo(location)

                    JSON_REPORT_NAME -> JsonReporter(outdated = outdated, latest = latest).writeTo(location)
                    HTML_REPORT_NAME -> HtmlReporter(outdated = outdated, latest = latest).writeTo(location)
                    else -> throw IllegalStateException("${report.name} report is not configured")
                }
            }
        }

        private fun PackageReporter.writeTo(file: File) {
            logger.info("Writing ${this::class.simpleName} to ${file.absolutePath}")
            file.parentFile.mkdirs()
            file.outputStream().use {
                writePackages(it)
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

        private val _reports: ReporterContainer = ReporterContainerImpl(this, CollectionCallbackActionDecorator.NOOP)

        @Internal
        override fun getReports() = _reports

        override fun reports(closure: Closure<*>): ReporterContainer =
            reports.apply {
                project.configure(this, closure)
            }

        override fun reports(configureAction: Action<in ReporterContainer>): ReporterContainer =
            reports.apply {
                configureAction.execute(this)
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
