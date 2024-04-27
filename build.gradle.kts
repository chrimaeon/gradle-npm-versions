/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Date
import java.util.Properties

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    idea
    kotlin("jvm") version embeddedKotlinVersion
    kotlin("plugin.serialization") version embeddedKotlinVersion
    alias(libs.plugins.versions)
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
            srcDirs(sourceSets.main.get().resources.srcDirs)
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

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            artifactId = pomArtifactId
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
    }

    check {
        dependsOn(functionalTest)
    }

    test {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to pomName,
                    "Implementation-Version" to versionName,
                    "Implementation-Vendor" to "CMG Mobile Apps",
                    "Created-By" to """${System.getProperty("java.version")} (${System.getProperty("java.vendor")})""",
                    "Build-By" to System.getProperty("user.name"),
                    "Build-Date" to Date(),
                    "Build-JDK" to System.getProperty("java.version"),
                    "Build-Gradle" to gradle.gradleVersion,
                    "Build-Kotlin" to libs.versions.kotlin,
                ),
            )
        }
    }

    named<DependencyUpdatesTask>("dependencyUpdates") {
        revision = "release"

        gradleReleaseChannel = GradleReleaseChannel.CURRENT.id

        rejectVersionIf {
            listOf("alpha", "beta", "rc", "cr", "m", "preview")
                .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                .any { it.matches(candidate.version) }
        }
    }
}

dependencies {
    compileOnly(libs.kotlin.gradle)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.serialization)
    implementation(libs.maven.artifact)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter) {
        exclude(group = "org.hamcrest")
    }
    testImplementation(libs.hamcrest)
    testImplementation(libs.kotlin.gradle)

    "functionalTestImplementation"(platform(libs.junit.bom))
    "functionalTestImplementation"(libs.junit.jupiter) {
        exclude(group = "org.hamcrest")
    }
    "functionalTestCompileOnly"(libs.kotlin.gradle)
    "functionalTestImplementation"(libs.hamcrest)
    "functionalTestImplementation"(gradleTestKit())
}
