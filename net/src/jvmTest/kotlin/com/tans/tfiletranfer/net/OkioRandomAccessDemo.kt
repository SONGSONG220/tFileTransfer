package com.tans.tfiletranfer.net

import okio.FileSystem
import okio.Path.Companion.toPath

object OkioRandomAccessDemo {

    suspend fun run() {
        val fs = FileSystem.SYSTEM
        val path = "build/okio-random-access-demo.bin".toPath()
        fs.createDirectories(path.parent!!)
        fs.write(path) {
            writeUtf8("0123456789abcdef")
        }
        val handle = fs.openReadWrite(path)
        val data1 = "KMP".encodeToByteArray()
        handle.write(4, data1, 0, data1.size)
        val data2 = "OKIO".encodeToByteArray()
        handle.write(10, data2, 0, data2.size)
        val bufAll = ByteArray(fs.metadata(path).size!!.toInt())
        handle.read(0, bufAll, 0, bufAll.size)
        println("All: " + bufAll.decodeToString())
        val bufSlice = ByteArray(8)
        handle.read(8, bufSlice, 0, bufSlice.size)
        println("Slice@8: " + bufSlice.decodeToString())
        handle.resize(64)
        val tail = ByteArray(16)
        handle.read(48, tail, 0, tail.size)
        println("Tail@48: " + tail.decodeToString())
        handle.close()
    }
}

