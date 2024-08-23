/*
 * Copyright (c) 2022. Christian Grach <christian.grach@cmgapps.com>
 */

plugins {
    `kotlin-dsl`
}

group = "com.cmgapps.website.convention.plugin"

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("cmgappsTestConvenvtion") {
            id = "cmgapps.gradle.test"
            implementationClass = "com.cmgapps.gradle.test.TestConvention"
        }
    }
}
