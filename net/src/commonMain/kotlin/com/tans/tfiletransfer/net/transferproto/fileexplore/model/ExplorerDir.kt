package com.tans.tfiletransfer.net.transferproto.fileexplore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * example /a/b/c
 *  name=c
 *  path=/a/b/c
 */
@Serializable
data class ExplorerDir(
    @SerialName("name")
    val name: String,
    @SerialName("path")
    val path: String,
    @SerialName("childrenCount")
    val childrenCount: Int,
    @SerialName("lastModify")
    val lastModify: Long
)