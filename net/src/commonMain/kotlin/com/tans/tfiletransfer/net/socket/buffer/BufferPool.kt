package com.tans.tfiletransfer.net.socket.buffer

// 5 MB
const val DEFAULT_MAX_POOL_SIZE = 1024L * 1024L * 5L

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class BufferPool {

    constructor(maxPoolSize: Long = DEFAULT_MAX_POOL_SIZE)

    fun get(requestSize: Int): Buffer

    fun put(buffer: Buffer)

    fun clearMemory()
}