/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.semver4j.Semver

@Serializable
data class NpmResponse(
    val name: String,
    val version: String,
)

@Serializable
data class Package(
    val name: String,
    @Serializable(with = SemverSerializer::class)
    val currentVersion: Semver,
    @Serializable(with = SemverSerializer::class)
    val availableVersion: Semver,
)

class SemverSerializer : KSerializer<Semver> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ComparableVersion", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Semver =
        Semver.parse(decoder.decodeString())
            ?: throw IllegalArgumentException("${decoder.decodeString()} is not a valid semantic version")

    override fun serialize(
        encoder: Encoder,
        value: Semver,
    ) = encoder.encodeString(value.toString())
}
