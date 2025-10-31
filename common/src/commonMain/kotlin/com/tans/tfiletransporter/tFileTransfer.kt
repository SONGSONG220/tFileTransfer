package com.tans.tfiletransporter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Suppress("ComposableNaming")
@Composable
fun tFileTransfer() {
    tFileTransferTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = platform(), modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Suppress("ComposableNaming")
@Composable
fun tFileTransferTheme(content: @Composable () -> Unit) {
    // TODO: set colors.
    MaterialTheme (content = content)
}