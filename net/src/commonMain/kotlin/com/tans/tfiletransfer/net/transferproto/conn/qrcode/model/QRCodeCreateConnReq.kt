package com.tans.tfiletransfer.net.transferproto.conn.qrcode.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QRCodeCreateConnReq(
    @SerialName("version")
    val version: Int,
    @SerialName("deviceName")
    val deviceName: String
)