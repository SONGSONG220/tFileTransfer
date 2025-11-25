package com.tans.tfiletransfer.net.socket.ext.converter

import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import kotlin.reflect.KClass

interface ITypeConverter {

    fun couldHandle(type: Int, typeClass: KClass<*>): Boolean

    fun <T : Any> convert(
        type: Int,
        typeClass: KClass<T>,
        pkt: PackageData,
        bufferPool: BufferPool
    ): T
}