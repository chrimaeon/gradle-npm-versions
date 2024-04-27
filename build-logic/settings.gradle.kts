/*
* Copyright (c) 2022. Christian Grach <christian.grach@cmgapps.com>
*/

@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include(
    ":plugin",
)
