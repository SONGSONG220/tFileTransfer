package com.tans.tfiletransporter

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual fun platform() = "Android"

actual fun ioDispatcher(): CoroutineContext = Dispatchers.IO