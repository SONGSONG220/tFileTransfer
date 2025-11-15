package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanDirRsp(
    @SerialName("path")
    val path: String,
    @SerialName("childrenDirs")
    val childrenDirs: List<FileExploreDir>,
    @SerialName("childrenFiles")
    val childrenFiles: List<FileExploreFile>
)