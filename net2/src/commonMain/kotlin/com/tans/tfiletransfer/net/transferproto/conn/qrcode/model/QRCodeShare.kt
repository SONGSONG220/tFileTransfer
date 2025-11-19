package com.tans.tfiletransfer.net.transferproto.conn.qrcode.model

import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QRCodeShare(
    @SerialName("version")
    val version: Int = TransferProtoConstant.VERSION,
    @SerialName("deviceName")
    val deviceName: String,
    @SerialName("address")
    val address: Int
)