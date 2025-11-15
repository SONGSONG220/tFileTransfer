package com.tans.tfiletransfer.net.transferproto.filetransfer.model

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.FileExploreFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadReq(
    @SerialName("file")
    val file: FileExploreFile,
    @SerialName("start")
    val start: Long,
    @SerialName("end")
    val end: Long
)