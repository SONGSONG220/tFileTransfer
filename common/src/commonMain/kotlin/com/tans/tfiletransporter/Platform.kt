package com.tans.tfiletransporter

import kotlin.coroutines.CoroutineContext


expect fun platform(): String


expect fun ioDispatcher(): CoroutineContext