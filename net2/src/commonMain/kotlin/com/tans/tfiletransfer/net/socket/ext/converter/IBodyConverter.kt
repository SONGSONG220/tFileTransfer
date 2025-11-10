package com.tans.tfiletransfer.net.socket.ext.converter

import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import kotlin.reflect.KClass

interface IBodyConverter {

    fun couldHandle(type: Int, dataClass: KClass<*>): Boolean

    fun <T : Any> convert(
        type: Int,
        dataClass: KClass<T>,
        packageData: PackageData,
        bufferPool: BufferPool
    ): T?
}