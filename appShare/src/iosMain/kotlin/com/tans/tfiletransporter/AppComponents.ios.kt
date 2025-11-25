package com.tans.tfiletransporter

import androidx.compose.runtime.Composable
import com.tans.tfiletransfer.net.NetLog

@Composable
actual fun initNetLog() {
    NetLog.init(Unit)
}

