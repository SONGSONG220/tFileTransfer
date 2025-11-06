@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.tans.tfiletransporter

import kotlin.coroutines.CoroutineContext

expect class PlatformContext

enum class Platform {
    Android,
    JVM
}

expect fun platform(): Platform

expect fun ioDispatcher(): CoroutineContext