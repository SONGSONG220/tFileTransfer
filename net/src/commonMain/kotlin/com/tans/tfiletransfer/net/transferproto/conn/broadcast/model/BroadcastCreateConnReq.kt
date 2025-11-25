package com.tans.tfiletransfer.net.transferproto.conn.broadcast.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastCreateConnReq(
    @SerialName("version")
    val version: Int,
    @SerialName("deviceName")
    val deviceName: String
)