/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class NpmVersionsExtension
    @Inject
    constructor(
        project: Project,
        objects: ObjectFactory,
    ) {
        val plainText: Reporter =
            Reporter(
                enabled = objects.property<Boolean>().convention(true),
                outputFile =
                    objects.fileProperty().convention(
                        project.layout.buildDirectory.file("npmVersions/report.txt"),
                    ),
            )

        fun plainText(action: Action<in Reporter>) = action.execute(plainText)

        val json: Reporter =
            Reporter(
                enabled = objects.property<Boolean>().convention(false),
                outputFile =
                    objects.fileProperty().convention(
                        project.layout.buildDirectory.file("npmVersions/report.json"),
                    ),
            )

        fun json(action: Action<in Reporter>) = action.execute(json)

        val html: Reporter =
            Reporter(
                enabled = objects.property<Boolean>().convention(false),
                outputFile =
                    objects.fileProperty().convention(
                        project.layout.buildDirectory.file("npmVersions/report.html"),
                    ),
            )

        fun html(action: Action<in Reporter>) = action.execute(html)

        val xml: Reporter =
            Reporter(
                enabled = objects.property<Boolean>().convention(false),
                outputFile =
                    objects.fileProperty().convention(
                        project.layout.buildDirectory.file("npmVersions/report.xml"),
                    ),
            )

        fun xml(action: Action<in Reporter>) = action.execute(xml)
    }

class Reporter(
    val enabled: Property<Boolean>,
    val outputFile: RegularFileProperty,
)
