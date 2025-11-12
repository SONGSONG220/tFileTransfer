package com.tans.tfiletransporter

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.tans.tfiletransfer.net.NetLog

@SuppressLint("ComposableNaming")
@Composable
actual fun initNetLog() {
    NetLog.init(LocalActivity.current!!)
}