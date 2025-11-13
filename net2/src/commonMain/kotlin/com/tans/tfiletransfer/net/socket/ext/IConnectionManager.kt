package com.tans.tfiletransfer.net.socket.ext

import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory

interface IConnectionManager {
    val connectionTask: IConnectionTask

    val converterFactory: IConverterFactory
}