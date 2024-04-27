/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.model

import kotlinx.serialization.Serializable

@Serializable
data class NpmResponse(
    val name: String,
    val version: String,
)

@Serializable
data class Package(
    val name: String,
    val currentVersion: String,
    val availableVersion: String,
)
