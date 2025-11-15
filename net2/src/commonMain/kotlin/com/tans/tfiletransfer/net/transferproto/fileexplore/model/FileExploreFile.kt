package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileExploreFile(
    @SerialName("name")
    val name: String,
    @SerialName("path")
    val path: String,
    @SerialName("size")
    val size: Long,
    @SerialName("lastModify")
    val lastModify: Long
)