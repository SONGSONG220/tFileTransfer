package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileExploreDir(
    @SerialName("name")
    val name: String,
    @SerialName("path")
    val path: String,
    @SerialName("childrenCount")
    val childrenCount: Int,
    @SerialName("lastModify")
    val lastModify: Long
)