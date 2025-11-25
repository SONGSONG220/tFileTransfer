package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendFilesReq(
    @SerialName("sendFiles")
    val sendFiles: List<ExplorerFile>,
    @SerialName("maxConnection")
    val maxConnection: Int
)