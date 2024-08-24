/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import org.semver4j.Semver

val outdatedPackages =
    listOf(
        Package(
            name = "outdated lib",
            currentVersion = Semver.of(1, 0, 0),
            availableVersion = Semver.of(2, 0, 0),
        ),
    )

val latestPackages =
    listOf(
        Package(
            name = "latest list",
            currentVersion = Semver.of(1, 0, 0),
            availableVersion = Semver.of(1, 0, 0),
        ),
    )
