/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    kotlin("multiplatform") version "1.9.23"
    id("com.cmgapps.npm.versions") version "1.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("org.apache.maven:maven-model:3.6.3")
            }
        }

        named("jvmMain") {
            dependencies {
                implementation("org.apache.commons:commons-csv:1.9.0")
            }
        }

        named("jsMain") {
            dependencies {
                implementation("org.apache.commons:commons-csv:1.9.0")
                implementation(npm("bootstrap", "5.3.3"))
                implementation(npm("kotlin", "1.0"))
            }
        }
    }
}
