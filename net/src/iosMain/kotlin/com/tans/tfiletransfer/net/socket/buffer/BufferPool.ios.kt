package com.tans.tfiletransfer.net.socket.buffer

import com.tans.tfiletransfer.net.socket.buffer.Buffer

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BufferPool actual constructor(maxPoolSize: Long) {

    actual fun get(requestSize: Int): Buffer {
        val size = requestSize.coerceAtLeast(0)
        return Buffer(ByteArray(size), 0)
    }

    actual fun put(buffer: Buffer) {
        // no-op on iOS placeholder
    }

    actual fun clearMemory() {
        // no-op on iOS placeholder
    }
}
