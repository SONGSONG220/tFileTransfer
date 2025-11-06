package com.tans.tfiletransporter.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp


@Composable
inline fun SpacerHeight(height: Dp) {
    Spacer(modifier = Modifier.height(height = height))
}

@Composable
inline fun SpacerWidth(width: Dp) {
    Spacer(modifier = Modifier.width(width = width))
}