package com.tans.tfiletransporter

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual fun platform(): String = "Jvm"

actual fun ioDispatcher(): CoroutineContext = Dispatchers.IO