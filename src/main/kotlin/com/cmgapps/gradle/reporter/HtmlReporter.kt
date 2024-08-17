/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.reporter

import com.cmgapps.gradle.dsl.Tag
import com.cmgapps.gradle.dsl.TagWithText
import com.cmgapps.gradle.model.Package
import java.io.OutputStream
import java.io.PrintStream

internal class HtmlReporter(
    outdated: List<Package>,
    latest: List<Package>,
) : PackageReporter(outdated = outdated, latest = latest) {
    override fun write(
        outputStream: PrintStream,
        text: String,
    ) {
        outputStream.print(text)
    }

    override fun writePackages(outputStream: OutputStream) {
        val html =
            html {
                head {
                    title {
                        +"NPM Versions"
                    }
                    style {
                        +javaClass
                            .getResourceAsStream("/normalize.css")!!
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }
                    }
                    style {
                        +javaClass
                            .getResourceAsStream("/style.css")!!
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }
                    }
                }
                body {
                    h1 {
                        +"NPM Versions"
                    }
                    if (latest.isNotEmpty()) {
                        p {
                            +"The following packages are using the latest version"
                        }
                        table {
                            latest.forEach {
                                tr {
                                    td {
                                        +it.name
                                    }
                                    td {
                                        +it.currentVersion
                                    }
                                }
                            }
                        }
                    }
                    if (outdated.isNotEmpty()) {
                        p {
                            +"The following packages have updated versions"
                        }
                        table {
                            outdated.forEach {
                                tr {
                                    td {
                                        +it.name
                                    }
                                    td {
                                        +"${it.currentVersion} &rarr; ${it.availableVersion}"
                                    }
                                }
                            }
                        }
                    }
                    p {
                        attributes["style"] = "text-align:right"
                        small {
                            +"Generated with "
                            a(href = "https://plugins.gradle.org/plugin/com.cmgapps.npm.versions") {
                                +"NPM Versions Gradle Plugin"
                            }
                        }
                    }
                }
            }
        PrintStream(outputStream).use { printStream ->
            write(printStream, html.toString())
        }
    }
}

internal class HTML : TagWithText("html") {
    init {
        attributes["lang"] = "en"
    }

    fun head(init: Head.() -> Unit) = initTag(Head(), init)

    fun body(init: Body.() -> Unit) = initTag(Body(), init)

    override fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    ) {
        builder.append("<!DOCTYPE html>")
        if (format) {
            builder.append('\n')
        }
        super.render(builder, intent, format)
    }
}

internal class Head : TagWithText("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)

    fun meta(attrs: Map<String, String>) {
        val meta = initTag(Meta()) {}
        meta.attributes.putAll(attrs)
    }

    fun style(init: Style.() -> Unit) = initTag(Style(), init)
}

internal class Title : TagWithText("title")

internal class Meta : Tag("meta") {
    override fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    ) {
        if (format) {
            builder.append(intent)
        }

        builder.append("<$name")
        for ((attr, value) in attributes) {
            builder.append(" $attr=\"$value\"")
        }
        builder.append(">")
        if (format) {
            builder.append('\n')
        }
    }
}

internal class Style : TagWithText("style")

internal abstract class BodyTag(
    name: String,
) : TagWithText(name) {
    fun pre(init: Pre.() -> Unit) = initTag(Pre(), init)

    fun h1(init: H1.() -> Unit) = initTag(H1(), init)

    fun ul(init: Ul.() -> Unit) = initTag(Ul(), init)

    fun li(init: Li.() -> Unit) = initTag(Li(), init)

    fun a(
        href: String,
        init: A.() -> Unit,
    ) {
        val a = initTag(A(), init)
        a.href = href
    }

    fun div(
        `class`: String = "",
        init: Div.() -> Unit,
    ) {
        val div = initTag(Div(), init)
        if (`class`.isNotEmpty()) {
            div.`class` = `class`
        }
    }

    fun p(init: P.() -> Unit) = initTag(P(), init)

    fun table(init: Table.() -> Unit) = initTag(Table(), init)

    fun small(init: Small.() -> Unit) = initTag(Small(), init)
}

internal class Body : BodyTag("body")

internal class Pre : BodyTag("pre")

internal class H1 : BodyTag("h1")

internal class Ul : BodyTag("ul")

internal class Li : BodyTag("li")

internal class A : BodyTag("a") {
    var href: String
        get() = attributes["href"]!!
        set(value) {
            attributes["href"] = value
        }

    override fun render(
        builder: StringBuilder,
        intent: String,
        format: Boolean,
    ) {
        if (format) {
            builder.append(intent)
        }
        builder.append("<a").append(renderAttributes()).append(">")
        for (c in children) {
            c.render(builder, "", false)
        }
        builder.append("</a>")
        if (format) {
            builder.append('\n')
        }
    }
}

internal class Small : BodyTag("small")

internal class Div : BodyTag("div") {
    var `class`: String
        get() = attributes["class"]!!
        set(value) {
            attributes["class"] = value
        }
}

internal class P : BodyTag("p")

internal abstract class TableTag(
    name: String,
) : TagWithText(name) {
    fun tr(init: Tr.() -> Unit) = initTag(Tr(), init)
}

internal abstract class TrTag(
    name: String,
) : TagWithText(name) {
    fun td(init: Td.() -> Unit) = initTag(Td(), init)
}

internal class Table : TableTag("table")

internal class Tr : TrTag("tr")

internal class Td : TagWithText("td")

internal fun html(init: HTML.() -> Unit): HTML = HTML().apply(init)
