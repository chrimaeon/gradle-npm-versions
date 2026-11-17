/*
 * Copyright (c) 2026. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21"
    id("com.cmgapps.npm.versions")
}

kotlin {
    js {
        browser()
    }

    sourceSets {
        named("jsMain") {
            dependencies {
                implementation(npm("bootstrap", "5.3.3"))
            }
        }
    }
}

npmVersions {
    npmRegistryUrl.set("http://localhost:8080")
}
