package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMsgReq(
    @SerialName("sendTime")
    val sendTime: Long,
    @SerialName("msg")
    val msg: String
)