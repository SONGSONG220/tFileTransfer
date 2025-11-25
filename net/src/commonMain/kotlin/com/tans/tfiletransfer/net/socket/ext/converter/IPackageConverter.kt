package com.tans.tfiletransfer.net.socket.ext.converter

import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import kotlin.reflect.KClass

interface IPackageConverter {

    fun couldHandle(type: Int, dataTypeClass: KClass<*>): Boolean

    fun <T : Any> convert(
        type: Int,
        messageId: Long,
        data: T,
        dataTypeClass: KClass<T>,
        bufferPool: BufferPool
    ): PackageData
}