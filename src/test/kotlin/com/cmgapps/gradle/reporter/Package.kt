/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package

val outdatedPackages =
    listOf(
        Package(
            name = "outdated lib",
            currentVersion = "1.0.0",
            availableVersion = "2.0.0",
        ),
    )

val latestPackages =
    listOf(
        Package(
            name = "latest list",
            currentVersion = "1.0.0",
            availableVersion = "1.0.0",
        ),
    )
