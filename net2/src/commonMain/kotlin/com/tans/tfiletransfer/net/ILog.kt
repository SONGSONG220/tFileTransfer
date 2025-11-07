package com.tans.tfiletransfer.net

interface ILog {

    fun d(tag: String, msg: String)

    fun i(tag: String, msg: String)

    fun w(tag: String, msg: String)

    fun e(tag: String, msg: String, throwable: Throwable? = null)
}