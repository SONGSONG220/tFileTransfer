package com.tans.tfiletransfer.net.transferproto.conn.broadcast.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastCreateConnRsp(
    @SerialName("deviceName")
    val deviceName: String
)