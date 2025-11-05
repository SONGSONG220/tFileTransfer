@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package com.tans.tfiletransporter

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual typealias PlatformContext = Activity

actual fun platform() = Platform.Android

actual fun ioDispatcher(): CoroutineContext = Dispatchers.IO