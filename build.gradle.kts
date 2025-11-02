/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.util.Date
import java.util.Properties

plugins {
    `java-gradle-plugin`
    `maven-publish`
    idea
    kotlin("jvm") version embeddedKotlinVersion
    kotlin("plugin.serialization") version embeddedKotlinVersion
    alias(libs.plugins.versions)
    id("ktlint")
    alias(libs.plugins.jetbrains.changelog)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlinx.kover)
    id("cmgapps.gradle.test")
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

val functionalTestSourceSet: SourceSet =
    sourceSets.create("functionalTest") {
        val sourceSetName = name
        kotlin {
            srcDir("src/$sourceSetName/kotlin")
        }
        resources {
            srcDirs(
                sourceSets.main
                    .get()
                    .resources.srcDirs,
            )
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

    testSourceSets(functionalTestSourceSet)
}

idea {
    module {
        testSources.from(functionalTestSourceSet.kotlin.srcDirs)
        testResources.from(functionalTestSourceSet.resources.srcDirs)
    }
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
    useJacoco()
    jacocoVersion = libs.versions.jacoco
    currentProject {
        sources {
            excludedSourceSets.addAll(functionalTestSourceSet.name)
        }
    }

    reports {
        filters {
            excludes {
                annotatedBy("kotlinx.serialization.Serializable")
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

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = libs.versions.gradle.get()
    }

    val functionalTest by registering(Test::class) {
        group = "verification"
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
        useJUnitPlatform()

        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
        }
    }

    check {
        dependsOn(functionalTest)
    }

    test {
        useJUnitPlatform()
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

    withType<DependencyUpdatesTask> {
        revision = "release"

        gradleReleaseChannel = GradleReleaseChannel.CURRENT.id

        rejectVersionIf {
            listOf("alpha", "beta", "rc", "cr", "m", "preview")
                .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                .any { it.matches(candidate.version) }
        }
    }

    val updateReadme by registering {
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
    compileOnly(libs.kotlin.gradle)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.serialization)
    implementation(libs.semver)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter) {
        exclude(group = "org.hamcrest")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.hamcrest)
    testImplementation(libs.kotlin.gradle)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.networknt.jsonschemavalidator)

    "functionalTestImplementation"(platform(libs.junit.bom))
    "functionalTestImplementation"(libs.junit.jupiter) {
        exclude(group = "org.hamcrest")
    }
    "functionalTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")

    "functionalTestImplementation"(libs.kotlin.gradle)
    "functionalTestImplementation"(libs.hamcrest)
    "functionalTestImplementation"(gradleTestKit())
}
