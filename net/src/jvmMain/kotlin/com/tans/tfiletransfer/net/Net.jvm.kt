@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.tans.tfiletransfer.net

actual object NetLog {

    actual fun init(context: PlatformContext) {
    }

    actual fun d(tag: String, msg: String) {
        println(" D $tag: $msg")
    }

    actual fun i(tag: String, msg: String) {
        println(" I $tag: $msg")
    }

    actual fun w(tag: String, msg: String) {
        println(" W $tag: $msg")
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        println(" E $tag: $msg")
        throwable?.printStackTrace()
    }
}