@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.tans.tfiletransfer.net

import platform.Foundation.NSLog

actual object NetLog {

    actual fun init(context: PlatformContext) {
        // no-op
    }

    actual fun d(tag: String, msg: String) {
        NSLog("D/%@: %@", tag, msg)
    }

    actual fun i(tag: String, msg: String) {
        NSLog("I/%@: %@", tag, msg)
    }

    actual fun w(tag: String, msg: String) {
        NSLog("W/%@: %@", tag, msg)
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        val t = throwable?.message ?: ""
        NSLog("E/%@: %@ %@", tag, msg, t)
    }
}
