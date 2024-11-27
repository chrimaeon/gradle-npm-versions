/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.JSON_REPORT_NAME
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.io.OutputStream
import java.io.PrintStream
import javax.inject.Inject

abstract class JsonReport
    @Inject
    constructor(
        name: String,
    ) : PackageSingleFileReport(name) {
        override fun getName(): String = JSON_REPORT_NAME

        private val json =
            Json {
                prettyPrint = true
            }

        override fun writePackages(outputStream: OutputStream) {
            val outdated =
                outdated.map {
                    Outdated(
                        name = it.name,
                        currentVersion = it.currentVersion.toString(),
                        latestVersion = it.availableVersion.toString(),
                    )
                }
            val latest =
                latest.map {
                    Latest(
                        name = it.name,
                        version = it.currentVersion.toString(),
                    )
                }

            val content =
                buildJsonObject {
                    put("latest", Json.encodeToJsonElement(latest))
                    put("outdated", Json.encodeToJsonElement(outdated))
                }

            PrintStream(outputStream).use { printStream ->
                printStream.println(json.encodeToString(content))
            }
        }
    }

@Serializable
private data class Outdated(
    val name: String,
    val currentVersion: String,
    val latestVersion: String,
)

@Serializable
private data class Latest(
    val name: String,
    val version: String,
)
