package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExploreDirRsp(
    @SerialName("path")
    val path: String,
    @SerialName("childrenDirs")
    val childrenDirs: List<ExplorerDir>,
    @SerialName("childrenFiles")
    val childrenFiles: List<ExplorerFile>
)