package com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WifiP2pHandshakeReq(
    @SerialName("version")
    val version: Int,
    @SerialName("deviceName")
    val deviceName: String
)