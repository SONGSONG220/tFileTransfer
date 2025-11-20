package com.tans.tfiletransfer.net.transferproto.filetransfer.model

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadReq(
    @SerialName("file")
    val file: ExplorerFile,
    @SerialName("start")
    val start: Long,
    @SerialName("end")
    val end: Long
)