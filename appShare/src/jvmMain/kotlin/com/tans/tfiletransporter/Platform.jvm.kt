@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.tans.tfiletransporter

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual typealias PlatformContext = Unit

actual fun platform() = Platform.JVM

actual fun ioDispatcher(): CoroutineContext = Dispatchers.IO