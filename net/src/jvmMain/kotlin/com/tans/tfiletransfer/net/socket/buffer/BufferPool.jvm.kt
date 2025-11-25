package com.tans.tfiletransfer.net.socket.buffer

import com.tans.tlrucache.memory.LruByteArrayPool
import java.util.concurrent.ConcurrentHashMap

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BufferPool {

    private val maxPoolSize: Long

    private val pool: LruByteArrayPool by lazy {
        LruByteArrayPool(maxPoolSize)
    }

    private val bufferToPoolBuffer = ConcurrentHashMap<Buffer, LruByteArrayPool.Companion.ByteArrayValue>()

    actual constructor(maxPoolSize: Long) {
        this.maxPoolSize = maxPoolSize
    }

    actual fun get(requestSize: Int): Buffer {
        if (requestSize <= 0) {
            return Buffer(
                array = ByteArray(0),
                contentSize = 0
            )
        }

        val size = if (requestSize % 1024 == 0) {
            requestSize
        } else {
            requestSize - (requestSize % 1024) + 1024
        }
        val poolBuffer = pool.get(size)
        val buffer = Buffer(array = poolBuffer.value, contentSize = 0)
        bufferToPoolBuffer[buffer] = poolBuffer
        return buffer
    }

    actual fun put(buffer: Buffer) {
        bufferToPoolBuffer.remove(buffer)?.let {
            if (it.value.isNotEmpty()) {
                pool.put(it)
            }
        }
    }

    actual fun clearMemory() {
        bufferToPoolBuffer.clear()
        pool.clearMemory()
    }
}