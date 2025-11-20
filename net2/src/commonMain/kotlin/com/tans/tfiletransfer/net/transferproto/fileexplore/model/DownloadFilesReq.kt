package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadFilesReq(
    @SerialName("downloadFiles")
    val downloadFiles: List<ExplorerFile>,
    @SerialName("bufferSize")
    val bufferSize: Int
)
