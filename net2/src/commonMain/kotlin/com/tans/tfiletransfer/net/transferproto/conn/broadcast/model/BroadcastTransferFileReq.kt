package com.tans.tfiletransfer.net.transferproto.conn.broadcast.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastTransferFileReq(
    @SerialName("version")
    val version: Int,
    @SerialName("deviceName")
    val deviceName: String
)