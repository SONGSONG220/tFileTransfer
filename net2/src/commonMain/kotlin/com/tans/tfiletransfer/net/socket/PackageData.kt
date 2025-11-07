package com.tans.tfiletransfer.net.socket

import com.tans.tfiletransfer.net.socket.buffer.Buffer

class PackageData(
    val type: Int,
    val messageId: Long,
    val data: Buffer
)