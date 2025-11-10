package com.tans.tfiletransfer.net.socket.ext.converter

import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import kotlin.reflect.KClass

interface IPackageDataConverter {

    fun couldHandle(type: Int, dataClass: KClass<*>): Boolean

    fun <T : Any> convert(
        type: Int,
        messageId: Long,
        data: T,
        dataClass: KClass<T>,
        bufferPool: BufferPool): PackageData?
}