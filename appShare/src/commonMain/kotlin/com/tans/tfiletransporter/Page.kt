package com.tans.tfiletransporter

import kotlinx.serialization.Serializable

sealed class Page {

    @Serializable
    data class  HomePage(val shareFileUrl: String? = null) : Page()

    @Serializable
    data class ConnectionPage(val s: String) : Page()
}