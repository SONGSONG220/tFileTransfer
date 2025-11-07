package com.tans.tfiletransfer.net.socket

sealed class ConnectionTaskState {
    data object Init : ConnectionTaskState()
    data object Connecting : ConnectionTaskState()
    data object Connected : ConnectionTaskState()
    data class Closed(val cause: String?) : ConnectionTaskState()
    data class Error(val throwable: Throwable?) : ConnectionTaskState()
}