/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import com.cmgapps.gradle.model.NpmResponse
import com.cmgapps.gradle.model.Package
import com.cmgapps.gradle.reporter.HtmlReport
import com.cmgapps.gradle.reporter.JsonReport
import com.cmgapps.gradle.reporter.PackageSingleFileReport
import com.cmgapps.gradle.reporter.TextReport
import com.cmgapps.gradle.reporter.XmlReport
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
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.reporting.Reporting
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
import java.io.FileOutputStream
import javax.inject.Inject

interface PackageReportContainer : ReportContainer<PackageSingleFileReport> {
    @get:Internal
    val plainText: TextReport

    @get:Internal
    val json: JsonReport

    @get:Internal
    val html: HtmlReport

    @get:Internal
    val xml: XmlReport
}

internal const val PLAIN_TEXT_REPORT_NAME = "plainText"
internal const val JSON_REPORT_NAME = "json"
internal const val HTML_REPORT_NAME = "html"
internal const val XML_REPORT_NAME = "xml"

internal class PackageReportContainerImpl(
    task: Task,
    collectionCallbackActionDecorator: CollectionCallbackActionDecorator,
) : TaskReportContainer<PackageSingleFileReport>(
        PackageSingleFileReport::class.java,
        task,
        collectionCallbackActionDecorator,
    ),
    PackageReportContainer {
    init {
        add(TextReport::class.java, PLAIN_TEXT_REPORT_NAME, task)
        add(JsonReport::class.java, JSON_REPORT_NAME, task)
        add(HtmlReport::class.java, HTML_REPORT_NAME, task)
        add(XmlReport::class.java, XML_REPORT_NAME, task)
    }

    override val plainText: TextReport
        get() = getByName(PLAIN_TEXT_REPORT_NAME) as TextReport

    override val json: JsonReport
        get() = getByName(JSON_REPORT_NAME) as JsonReport

    override val html: HtmlReport
        get() = getByName(HTML_REPORT_NAME) as HtmlReport

    override val xml: XmlReport
        get() = getByName(XML_REPORT_NAME) as XmlReport
}

abstract class NpmVersionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
        objects: ObjectFactory,
    ) : DefaultTask(),
        Reporting<PackageReportContainer> {
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

            reports.configureEach {
                this.outdated = outdated
                this.latest = latest
            }

            reports.plainText.writePackages(System.out)

            reports.filter { it.required.get() }.forEach { report ->
                (report as PackageSingleFileReport).write()
            }
        }

        private fun PackageSingleFileReport.write() {
            with(outputLocation.get().asFile) {
                parentFile.mkdirs()
                outputStream().use {
                    writePackages(it)
                    logger.info("${this.name} report saved to $absolutePath")
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

        private val _reports: PackageReportContainer =
            PackageReportContainerImpl(this, CollectionCallbackActionDecorator.NOOP)

        @Internal
        override fun getReports(): PackageReportContainer = _reports

        override fun reports(closure: Closure<*>): PackageReportContainer =
            reports.apply {
                project.configure(this, closure)
            }

        override fun reports(configureAction: Action<in PackageReportContainer>): PackageReportContainer =
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
