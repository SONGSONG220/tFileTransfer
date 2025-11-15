package com.tans.tfiletransfer.net.transferproto.conn.broadcast.model

import com.tans.tfiletransfer.net.socket.Address

data class RemoteDevice(
    val remoteAddress: Address,
    val deviceName: String
)