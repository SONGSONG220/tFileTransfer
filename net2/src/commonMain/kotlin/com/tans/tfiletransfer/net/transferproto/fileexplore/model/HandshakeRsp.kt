package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HandshakeRsp(
    @SerialName("fileSeparator")
    val fileSeparator: String
)
