package com.tans.tfiletransfer.net.socket.ext.converter

import kotlin.reflect.KClass

interface IConverterFactory {

    fun findTypeConverter(type: Int, typeClass: KClass<*>) : ITypeConverter?

    fun findPackageConverter(type: Int, dataTypeClass: KClass<*>): IPackageConverter?
}