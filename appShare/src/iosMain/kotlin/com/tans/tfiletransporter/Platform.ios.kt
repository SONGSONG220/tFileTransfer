@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.tans.tfiletransporter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.coroutines.CoroutineContext

actual typealias PlatformContext = Unit

actual fun platform() = Platform.iOS

actual fun ioDispatcher(): CoroutineContext = Dispatchers.IO