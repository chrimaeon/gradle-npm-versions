/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import java.util.Date
import java.util.Properties

plugins {
    `java-gradle-plugin`
    `maven-publish`
    idea
    kotlin("jvm") version embeddedKotlinVersion
    kotlin("plugin.serialization") version embeddedKotlinVersion
    id("ktlint")
    alias(libs.plugins.jetbrains.changelog)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlinx.kover)
    id("cmgapps.gradle.test")
    alias(libs.plugins.buildconfig)
}

val pomProperties =
    Properties().apply {
        rootDir.resolve("pom.properties").inputStream().use {
            load(it)
        }
    }

val group: String by pomProperties
val versionName: String by pomProperties
val pomName: String by pomProperties
val projectUrl: String by pomProperties
val pomDescription: String by pomProperties
val scmUrl: String by pomProperties
val pomArtifactId: String by pomProperties

project.group = group
version = versionName

testing {
    suites {
        val test =
            named("test", JvmTestSuite::class) {
                useJUnitJupiter()
            }

        val functionalTestSuite =
            register<JvmTestSuite>("functionalTest") {
                dependencies {
                    implementation(project())
                    implementation(platform(libs.junit.bom))
                    implementation(libs.junit.jupiter) {
                        exclude(group = "org.hamcrest")
                    }
                    implementation(libs.hamcrest)
                    implementation(gradleTestKit())
                    implementation(libs.networknt.jsonschemavalidator)
                    implementation(libs.java.diff.utils)
                    implementation(libs.ktor.server.cio)
                    implementation(libs.ktor.server.content.negotiation)
                    implementation(libs.ktor.serialization.json)
                }

                targets.configureEach {
                    testTask.configure {
                        jvmArgs("-Xmx2g", "-Xms512m")
                        shouldRunAfter(test)
                    }
                }
            }

        tasks.check {
            dependsOn(functionalTestSuite)
        }
    }
}

gradlePlugin {
    website.set(projectUrl)
    vcsUrl.set(scmUrl)

    plugins {
        create("npmVersionPlugin") {
            id = "com.cmgapps.npm.versions"
            implementationClass = "com.cmgapps.gradle.NpmVersionsPlugin"
            displayName = pomName
            description = pomDescription
            tags.set(listOf("multiplatform", "Kotlin/JS", "NPM", "versioning"))
        }
    }

    testSourceSets(sourceSets["functionalTest"])
}

// HEY! If you update the minimum-supported Gradle version, check to see if the Kotlin language version or
// Java targets below can be bumped. See https://docs.gradle.org/current/userguide/compatibility.html.
val minimumGradleVersion = "9.0"
configurations.apiElements {
    attributes {
        attribute(
            GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
            objects.named(GradlePluginApiVersion::class.java, minimumGradleVersion),
        )
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
        jvmTarget = JvmTarget.JVM_17
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    }
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            artifactId = pomArtifactId
        }
    }
}

changelog {
    version.set(versionName)
    header.set(provider { version.get() })
}

kover {
    useJacoco = true
    jacocoVersion = libs.versions.jacoco
    currentProject {
        sources {
            excludedSourceSets.add(sourceSets["functionalTest"].name)
        }
    }

    reports {
        filters {
            excludes {
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }

        total {
            log {
                onCheck = true
                header = "Total Test Line Coverage"
                groupBy = GroupingEntityType.APPLICATION
                aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                coverageUnits = CoverageUnit.LINE
                format = "<value>% total line coverage"
            }
        }

        verify {
            rule("Minimal Line coverage") {
                bound {
                    minValue = 80
                    coverageUnits = CoverageUnit.LINE
                    aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                }
            }
        }
    }
}

buildConfig {
    sourceSets.named("functionalTest") {
        useKotlinOutput {
            packageName = "com.cmgapps.gradle"
            topLevelConstants = true
        }
        buildConfigField("MINIMUM_GRADLE_VERSION", minimumGradleVersion)
    }
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = libs.versions.gradle.get()
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to pomName,
                    "Implementation-Version" to versionName,
                    "Implementation-Vendor" to "CMG Mobile Apps",
                    "Created-By" to """${System.getProperty("java.version")} (${System.getProperty("java.vendor")})""",
                    "Built-By" to System.getProperty("user.name"),
                    "Built-Date" to Date(),
                    "Built-JDK" to System.getProperty("java.version"),
                    "Built-Gradle" to gradle.gradleVersion,
                    "Built-Kotlin" to libs.versions.kotlin,
                ),
            )
        }
    }

    koverVerify {
        dependsOn("ktlint")
    }

    val updateReadme =
        register("updateReadme") {
            description = "Updates the version in the README.md"
            val readmeFile = rootDir.resolve("README.md")
            val version: String = project.version as String

            inputs.property("libVersion", version)
            outputs.file(readmeFile)

            doLast {
                val content = readmeFile.readText()
                val oldVersion =
                    """id\("com.cmgapps.npm.versions"\) version "(.*)""""
                        .toRegex(RegexOption.MULTILINE)
                        .find(content)
                        ?.let {
                            it.groupValues[1]
                        } ?: error("Cannot find oldVersion")

                logger.info("Updating README.md version $oldVersion to $version")

                val newContent = content.replace(oldVersion, version)
                readmeFile.writeText(newContent)
            }
        }

    patchChangelog {
        dependsOn(updateReadme)
    }
}

dependencies {
    @Suppress("AvoidDuplicateDependencies")
    compileOnly(libs.kotlin.gradle)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.serialization)
    implementation(libs.semver)

    testImplementation(gradleApi())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter) {
        exclude(group = "org.hamcrest")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.hamcrest)
    @Suppress("AvoidDuplicateDependencies")
    testImplementation(libs.kotlin.gradle)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.networknt.jsonschemavalidator)
}
