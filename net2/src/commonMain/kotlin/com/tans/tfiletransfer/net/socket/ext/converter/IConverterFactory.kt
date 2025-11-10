package com.tans.tfiletransfer.net.socket.ext.converter

import kotlin.reflect.KClass

interface IConverterFactory {

    fun findBodyConverter(type: Int, dataClass: KClass<*>) : IBodyConverter?

    fun findPackageDataConverter(type: Int, dataClass: KClass<*>): IPackageDataConverter?
}