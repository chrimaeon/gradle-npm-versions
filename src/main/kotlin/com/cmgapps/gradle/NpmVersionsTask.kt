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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.internal.ConfigureUtil
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.semver4j.Semver
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
    private val objects: ObjectFactory,
) : NamedDomainObjectSet<PackageSingleFileReport> by objects.namedDomainObjectSet(PackageSingleFileReport::class.java),
    PackageReportContainer {
    init {
        add(TextReport::class.java, PLAIN_TEXT_REPORT_NAME)
        add(JsonReport::class.java, JSON_REPORT_NAME)
        add(HtmlReport::class.java, HTML_REPORT_NAME)
        add(XmlReport::class.java, XML_REPORT_NAME)
    }

    override fun getEnabled(): NamedDomainObjectSet<PackageSingleFileReport> = enabled

    @Override
    override fun getEnabledReports(): MutableMap<String, PackageSingleFileReport> = enabled.asMap

    private val enabled: NamedDomainObjectSet<PackageSingleFileReport> =
        matching { element -> element.required.get() }

    override fun configure(cl: Closure<*>?): ReportContainer<PackageSingleFileReport> {
        ConfigureUtil.configureSelf(
            cl,
            this,
        )
        return this
    }

    private fun add(
        clazz: Class<out PackageSingleFileReport>,
        name: String,
    ) {
        add(objects.newInstance(clazz, name))
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
        val networkService: Property<NetworkService> = objects.property(NetworkService::class.java)

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
                    .map { it.inputStream().use { stream -> Json.decodeFromStream<Package>(stream) } }
                    .partition { it.currentVersion < it.availableVersion }

            reports.configureEach { report ->
                report.outdated = outdated
                report.latest = latest
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
            // check for valid version
            try {
                Semver(dependency.version)
            } catch (e: Exception) {
                logger.warn("Could not parse version '${dependency.version}' for package '${dependency.name}'")
                return
            }
            submit(CheckNpmPackageAction::class.java) { params ->
                params.dependencyName.set(dependency.name)
                params.dependencyVersion.set(dependency.version)
                params.outputDirectory.set(this@NpmVersionTask.outputDirectory)
                params.networkService.set(this@NpmVersionTask.networkService)
            }
        }

        private val reports: PackageReportContainer = PackageReportContainerImpl(project.objects)

        @Internal
        override fun getReports(): PackageReportContainer = reports

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
    @OptIn(ExperimentalSerializationApi::class)
    override fun execute() {
        val outputDirectory = parameters.outputDirectory
        val dependencyName = parameters.dependencyName.get()
        val response =
            runBlocking {
                getLatestVersion(dependencyName)
            }

        FileOutputStream(
            outputDirectory.asFile.get().resolve(dependencyName.encodeURLPathPart() + ".json"),
        ).use {
            Json.encodeToStream(
                Package(
                    name = response.name,
                    currentVersion = Semver(parameters.dependencyVersion.get()),
                    availableVersion = Semver(response.version),
                ),
                it,
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
