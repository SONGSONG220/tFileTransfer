package com.tans.tfiletransporter

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

class IosEntry {
    fun rootViewController(): UIViewController = ComposeUIViewController { tFileTransfer() }
}

