/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.model.Package
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.io.OutputStream
import java.io.PrintStream

internal class JsonReporter(
    outdated: List<Package>,
    latest: List<Package>,
) : PackageReporter(outdated = outdated, latest = latest) {
    override fun write(
        outputStream: PrintStream,
        text: String,
    ) {
        outputStream.println(text)
    }

    private val json =
        Json {
            prettyPrint = true
        }

    override fun writePackages(outputStream: OutputStream) {
        val outdated =
            outdated.map {
                Outdated(
                    name = it.name,
                    currentVersion = it.currentVersion,
                    latestVersion = it.availableVersion,
                )
            }
        val latest =
            latest.map {
                Latest(
                    name = it.name,
                    version = it.currentVersion,
                )
            }

        val content =
            buildJsonObject {
                put("latest", Json.encodeToJsonElement(latest))
                put("outdated", Json.encodeToJsonElement(outdated))
            }

        write(
            PrintStream(outputStream),
            json.encodeToString(content),
        )
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
