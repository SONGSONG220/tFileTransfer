package com.tans.tfiletransporter.ui.home.wifidirect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.tans.tfiletransporter.LocalAppTypography
import com.tans.tfiletransporter.resources.Res
import com.tans.tfiletransporter.resources.wifi_p2p_connection_tips
import com.tans.tfiletransporter.resources.wifi_p2p_connection_title
import com.tans.tfiletransporter.ui.SpacerHeight
import com.tans.tfiletransporter.ui.home.homeScreenCardModifier
import org.jetbrains.compose.resources.stringResource


@Composable
actual fun ColumnScope.WiFiDirectConnection(backEntry: NavBackStackEntry) {

    Box(
        modifier = homeScreenCardModifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.wifi_p2p_connection_title),
                style = LocalAppTypography.current.titleMedium
            )
            SpacerHeight(5.dp)
            Text(
                text = stringResource(Res.string.wifi_p2p_connection_tips),
                style = LocalAppTypography.current.bodyMedium
            )
        }
    }
}