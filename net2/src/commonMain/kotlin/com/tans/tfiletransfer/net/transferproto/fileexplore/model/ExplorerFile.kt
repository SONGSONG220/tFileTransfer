package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * example /a/b/c.txt
 *  name=c.txt
 *  path=/a/b/c.txt
 */
@Serializable
data class ExplorerFile(
    @SerialName("name")
    val name: String,
    @SerialName("path")
    val path: String,
    @SerialName("size")
    val size: Long,
    @SerialName("lastModify")
    val lastModify: Long
)