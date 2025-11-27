package com.tans.tfiletransfer.net.socket.buffer

import com.tans.tfiletransfer.net.collections.AtomicList
import com.tans.tfiletransfer.net.collections.AtomicMap
import kotlinx.atomicfu.atomic

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BufferPool {

    private val maxPoolSize: Long

    private val freeBySize = AtomicMap<Int, AtomicList<ByteArray>>()
    private val bufferToArray = AtomicMap<Buffer, ByteArray>()
    private val freeTotalBytes = atomic(0L)

    actual constructor(maxPoolSize: Long) {
        this.maxPoolSize = maxPoolSize
    }

    actual fun get(requestSize: Int): Buffer {
        if (requestSize <= 0) {
            return Buffer(ByteArray(0), 0)
        }
        val size = if (requestSize % 1024 == 0) requestSize else requestSize - (requestSize % 1024) + 1024
        val list = freeBySize[size]
        val array = if (list != null && list.isNotEmpty()) {
            val idx = list.size - 1
            val v = list.removeAt(idx)
            freeTotalBytes.value = (freeTotalBytes.value - size).coerceAtLeast(0L)
            v
        } else {
            ByteArray(size)
        }
        val buffer = Buffer(array, 0)
        bufferToArray[buffer] = array
        return buffer
    }

    actual fun put(buffer: Buffer) {
        val array = bufferToArray.remove(buffer) ?: return
        val size = array.size
        if (size <= 0) return
        val list = freeBySize[size] ?: run {
            val newList = AtomicList<ByteArray>()
            freeBySize[size] = newList
            newList
        }
        list.add(array)
        freeTotalBytes.value += size
        if (freeTotalBytes.value > maxPoolSize) {
            evict()
        }
    }

    actual fun clearMemory() {
        bufferToArray.clear()
        val keys = freeBySize.keys.toList()
        for (k in keys) {
            freeBySize[k]?.clear()
        }
        freeBySize.clear()
        freeTotalBytes.value = 0L
    }

    private fun evict() {
        if (freeTotalBytes.value <= maxPoolSize) return
        val keys = freeBySize.keys.toList().sorted()
        var remaining = freeTotalBytes.value
        var i = 0
        while (remaining > maxPoolSize && i < keys.size) {
            val k = keys[i]
            val list = freeBySize[k]
            if (list != null && list.isNotEmpty()) {
                val removed = list.removeAt(0)
                remaining -= removed.size
            } else {
                i += 1
            }
        }
        freeTotalBytes.value = remaining.coerceAtLeast(0L)
    }
}
