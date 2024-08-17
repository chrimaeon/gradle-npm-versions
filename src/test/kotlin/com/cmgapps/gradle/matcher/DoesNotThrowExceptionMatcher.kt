/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.gradle.matcher

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class DoesNotThrowExceptionMatcher<T> : TypeSafeMatcher<() -> T>() {
    private var caughtException: Exception? = null

    override fun describeTo(description: Description) {
        description.appendText("no exception to be thrown")
    }

    override fun matchesSafely(supplier: () -> T): Boolean =
        try {
            supplier()
            true
        } catch (exc: Exception) {
            caughtException = exc
            false
        }

    override fun describeMismatchSafely(
        supplier: () -> T,
        mismatchDescription: Description,
    ) {
        caughtException?.let { exc ->
            mismatchDescription.appendText("a ${exc::class.java.name} was thrown: ${exc.message}")
        } ?: mismatchDescription.appendText("an exception was thrown")
    }

    companion object {
        fun <T> doesNotThrowException(): DoesNotThrowExceptionMatcher<T> = DoesNotThrowExceptionMatcher()
    }
}
