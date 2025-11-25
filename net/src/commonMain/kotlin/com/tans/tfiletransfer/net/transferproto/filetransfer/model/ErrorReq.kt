package com.tans.tfiletransfer.net.transferproto.filetransfer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorReq(
    @SerialName("errorMsg")
    val errorMsg: String
)