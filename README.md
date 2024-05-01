# Gradle Licenses Plugin [![Build & Test](https://github.com/chrimaeon/gradle-npm-versions/actions/workflows/main.yml/badge.svg)](https://github.com/chrimaeon/gradle-npm-versions/actions/workflows/main.yml)

[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg?style=for-the-badge)](http://www.apache.org/licenses/LICENSE-2.0)
[![Gradle Plugin](https://img.shields.io/badge/Gradle-8.0%2B-%2302303A.svg?style=for-the-badge&logo=Gradle)](https://gradle.org/)
[![gradlePluginPortal](https://img.shields.io/gradle-plugin-portal/v/com.cmgapps.npm.versions?label=Gradle%20Plugin%20Portal&style=for-the-badge&logo=Gradle)](https://plugins.gradle.org/plugin/com.cmgapps.licenses)

This Gradle plugin provides a task to check NPM package version for your [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) project.

## Usage

### Integration

#### Using the plugins DSL

<details open="open">
<summary>Kotlin</summary>

```kotlin
plugins {
    id("com.cmgapps.npm.versions") version "0.1.0"
}
```
</details>

<details>
<summary>Groovy</summary>

```groovy
plugins {
    id 'com.cmgapps.npm.versions' version '0.1.0'
}
```
</details>

#### Using legacy plugin application

<details open="open">
<summary>Kotlin</summary>

```kotlin
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.cmgapps.gradle:gradle-npm-versions-plugin:0.1.0")
    }
}

apply(plugin = "com.cmgapps.npm.versions")
```
</details>

<details>
<summary>Groovy</summary>

```groovy
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
    dependencies {
        classpath 'com.cmgapps.gradle:gradle-npm-versions-plugin:0.1.0'
    }
}

apply plugin: 'com.cmgapps.npm.versions'
```
</details>

### Task

Applying the plugin will create tasks to check for NPM Package Version

* `npmVersions`

This will check the [NPM](https://www.npmjs.com/) Registry for the latest version available for
your `npm` dependencies.

### Output

```text
┌──────────────┐
│ NPM Packages │
└──────────────┘

The following packages are using the latest version:
 · bootstrap:5.3.3

The following packages have updated versions:
 · kotlin [1.0 -> 1.9.23]

```


## License

```text
Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>

SPDX-License-Identifier: Apache-2.0
```