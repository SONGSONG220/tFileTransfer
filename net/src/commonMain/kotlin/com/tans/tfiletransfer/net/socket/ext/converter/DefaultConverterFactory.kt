package com.tans.tfiletransfer.net.socket.ext.converter

import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.Buffer
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

open class DefaultConverterFactory : IConverterFactory {

    override fun findTypeConverter(type: Int, typeClass: KClass<*>): ITypeConverter? {
        return defaultTypeConverters.find { it.couldHandle(type, typeClass) }
    }

    override fun findPackageConverter(type: Int, dataTypeClass: KClass<*>): IPackageConverter? {
        return defaultPackageConverters.find { it.couldHandle(type, dataTypeClass) }
    }


    companion object {

        private val defaultTypeConverters: List<ITypeConverter> = listOf(
            PackageTypeConverter(),
            StringTypeConverter(),
            UnitTypeConverter(),
            BufferTypeConverter(),
            JsonTypeConverter()
        )


        @Suppress("UNCHECKED_CAST")
        private class PackageTypeConverter : ITypeConverter {
            override fun couldHandle(type: Int, typeClass: KClass<*>): Boolean {
                return typeClass == PackageData::class
            }

            override fun <T : Any> convert(
                type: Int,
                typeClass: KClass<T>,
                pkt: PackageData,
                bufferPool: BufferPool
            ): T {
                return pkt as T
            }
        }

        @Suppress("UNCHECKED_CAST")
        private class StringTypeConverter : ITypeConverter {
            override fun couldHandle(type: Int, typeClass: KClass<*>): Boolean {
                return typeClass == String::class
            }

            override fun <T : Any> convert(
                type: Int,
                typeClass: KClass<T>,
                pkt: PackageData,
                bufferPool: BufferPool
            ): T {
                val body = pkt.data
                val ret = body.array.decodeToString(startIndex = 0, endIndex = body.contentSize)
                bufferPool.put(body)
                return ret as T
            }
        }

        @Suppress("UNCHECKED_CAST")
        private class UnitTypeConverter : ITypeConverter {
            override fun couldHandle(type: Int, typeClass: KClass<*>): Boolean {
                return typeClass == Unit::class
            }

            override fun <T : Any> convert(
                type: Int,
                typeClass: KClass<T>,
                pkt: PackageData,
                bufferPool: BufferPool
            ): T {
                return Unit as T
            }
        }

        @Suppress("UNCHECKED_CAST")
        private class BufferTypeConverter : ITypeConverter {

            override fun couldHandle(type: Int, typeClass: KClass<*>): Boolean {
                return typeClass == Buffer::class
            }

            override fun <T : Any> convert(
                type: Int,
                typeClass: KClass<T>,
                pkt: PackageData,
                bufferPool: BufferPool
            ): T {
                return pkt.data as T
            }

        }

        private class JsonTypeConverter : ITypeConverter {

            override fun couldHandle(type: Int, typeClass: KClass<*>): Boolean = true

            @InternalSerializationApi
            override fun <T : Any> convert(
                type: Int,
                typeClass: KClass<T>,
                pkt: PackageData,
                bufferPool: BufferPool
            ): T {
                val body = pkt.data
                val jsonString = body.array.decodeToString(startIndex = 0, endIndex = body.contentSize)
                bufferPool.put(body)
                val ret: T = Json.decodeFromString(typeClass.serializer(), jsonString)
                return ret
            }
        }

        private val defaultPackageConverters: List<IPackageConverter> = listOf(
            StringPackageConverter(),
            UnitPackageConverter(),
            BufferPackageConverter(),
            PackagePackageConverter(),
            JsonPackageConverter()
        )

        private class StringPackageConverter : IPackageConverter {

            override fun couldHandle(
                type: Int,
                dataTypeClass: KClass<*>
            ): Boolean {
                return dataTypeClass == String::class
            }

            override fun <T : Any> convert(
                type: Int,
                messageId: Long,
                data: T,
                dataTypeClass: KClass<T>,
                bufferPool: BufferPool
            ): PackageData {
                val bytes = (data as String).encodeToByteArray()
                return PackageData(
                    type = type,
                    messageId = messageId,
                    data = Buffer(
                        array = bytes,
                        contentSize = bytes.size
                    )
                )
            }
        }

        private class UnitPackageConverter : IPackageConverter {

            override fun couldHandle(
                type: Int,
                dataTypeClass: KClass<*>
            ): Boolean {
                return dataTypeClass == Unit::class
            }

            override fun <T : Any> convert(
                type: Int,
                messageId: Long,
                data: T,
                dataTypeClass: KClass<T>,
                bufferPool: BufferPool
            ): PackageData {
                return PackageData(
                    type = type,
                    messageId = messageId,
                    data = Buffer(
                        array = ByteArray(0),
                        contentSize = 0
                    )
                )
            }
        }

        private class BufferPackageConverter : IPackageConverter {

            override fun couldHandle(
                type: Int,
                dataTypeClass: KClass<*>
            ): Boolean {
                return dataTypeClass == Buffer::class
            }

            override fun <T : Any> convert(
                type: Int,
                messageId: Long,
                data: T,
                dataTypeClass: KClass<T>,
                bufferPool: BufferPool
            ): PackageData {
                return PackageData(
                    type = type,
                    messageId = messageId,
                    data = data as Buffer
                )
            }


        }

        private class PackagePackageConverter : IPackageConverter {

            override fun couldHandle(
                type: Int,
                dataTypeClass: KClass<*>
            ): Boolean {
                return dataTypeClass == PackageData::class
            }

            override fun <T : Any> convert(
                type: Int,
                messageId: Long,
                data: T,
                dataTypeClass: KClass<T>,
                bufferPool: BufferPool
            ): PackageData {
                return data as PackageData
            }
        }

        private class JsonPackageConverter : IPackageConverter {

            override fun couldHandle(
                type: Int,
                dataTypeClass: KClass<*>
            ): Boolean {
                return true
            }

            @OptIn(InternalSerializationApi::class)
            override fun <T : Any> convert(
                type: Int,
                messageId: Long,
                data: T,
                dataTypeClass: KClass<T>,
                bufferPool: BufferPool
            ): PackageData {
                val jsonStr = Json.encodeToString(dataTypeClass.serializer(), data)
                val bytes = jsonStr.encodeToByteArray()
                return PackageData(
                    type = type,
                    messageId = messageId,
                    data = Buffer(
                        array = bytes,
                        contentSize = bytes.size
                    )
                )
            }

        }
    }
}