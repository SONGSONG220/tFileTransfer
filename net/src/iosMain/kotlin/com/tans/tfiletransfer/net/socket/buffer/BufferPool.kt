package com.tans.tfiletransfer.net.socket.buffer

import com.tans.tfiletransfer.net.socket.buffer.Buffer

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BufferPool actual constructor(maxPoolSize: Long) {

    actual fun get(requestSize: Int): Buffer {
        TODO("Not yet implemented")
    }

    actual fun put(buffer: Buffer) {
        TODO("Not yet implemented")
    }

    actual fun clearMemory() {
        TODO("Not yet implemented")
    }
}

