package com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class P2pHandshakeRsp(
    @SerialName("deviceName")
    val deviceName: String
)