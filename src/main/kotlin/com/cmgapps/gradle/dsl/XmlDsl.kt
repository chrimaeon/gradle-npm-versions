/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.dsl

internal interface Element {
    fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    )
}

internal class TextElement(
    private val text: String,
) : Element {
    override fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    ) {
        if (format) {
            builder.append(intent)
        }
        builder.append(text)
        if (format) {
            builder.append('\n')
        }
    }
}

@DslMarker
internal annotation class HtmlTagMarker

@HtmlTagMarker
internal abstract class Tag(
    protected val name: String,
) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(
        tag: T,
        init: T.() -> Unit,
    ): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    ) {
        if (format) {
            builder.append(intent)
        }
        builder.append("<$name").append(renderAttributes())

        if (children.isEmpty()) {
            builder.append("/>")
            if (format) {
                builder.append('\n')
            }
            return
        }

        builder.append(">")

        if (format) {
            builder.append('\n')
        }

        for (c in children) {
            c.render(builder, "$intent  ", format)
        }

        if (format) {
            builder.append(intent)
        }
        builder.append("</$name>")
        if (format) {
            builder.append('\n')
        }
    }

    protected fun renderAttributes(): String =
        buildString {
            for ((attr, value) in attributes) {
                append(" $attr=\"$value\"")
            }
        }

    fun toString(format: Boolean = true): String =
        buildString {
            render(this, "", format)
        }

    override fun toString(): String = toString(true)
}

@HtmlTagMarker
internal abstract class TagWithText(
    name: String,
) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}
