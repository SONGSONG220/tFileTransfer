package com.tans.tfiletransfer.net.transferproto.conn.broadcast.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BroadcastMsg(
    @SerialName("version")
    val version: Int,
    @SerialName("deviceName")
    val deviceName: String
)
