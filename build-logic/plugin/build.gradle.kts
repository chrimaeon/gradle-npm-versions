/*
 * Copyright (c) 2022. Christian Grach <christian.grach@cmgapps.com>
 */

plugins {
    `kotlin-dsl`
}

group = "com.cmgapps.website.buildlogic.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
