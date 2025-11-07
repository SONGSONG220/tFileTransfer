@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.tans.tfiletransfer.net

import com.tans.tlog.tLog
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import java.io.File

actual object NetLog {

    private val realLog: AtomicRef<tLog?> = atomic(null)

    actual fun init(context: PlatformContext) {
        realLog.compareAndSet(null, tLog.Companion.Builder(baseDir = File(context.cacheDir, "tfiletransfer-net-log")).build())
    }

    actual fun d(tag: String, msg: String) {
        realLog.value?.d(tag, msg)
    }

    actual fun i(tag: String, msg: String) {
        realLog.value?.i(tag, msg)
    }

    actual fun w(tag: String, msg: String) {
        realLog.value?.w(tag, msg)
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        realLog.value?.w(tag, msg)
    }
}