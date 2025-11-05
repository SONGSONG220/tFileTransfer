package com.tans.tfiletransporter.ui.home.localnetwork

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.tans.tfiletransporter.LocalAppColorScheme
import com.tans.tfiletransporter.LocalAppTypography
import com.tans.tfiletransporter.resources.Res
import com.tans.tfiletransporter.resources.broadcast_connection_as_receiver
import com.tans.tfiletransporter.resources.broadcast_connection_as_sender
import com.tans.tfiletransporter.resources.local_network_connection_description
import com.tans.tfiletransporter.resources.local_network_connection_title
import com.tans.tfiletransporter.resources.local_network_scan_qr_code
import com.tans.tfiletransporter.resources.local_network_show_qr_code
import com.tans.tfiletransporter.ui.SpacerHeight
import com.tans.tfiletransporter.ui.home.homeScreenCardModifier
import org.jetbrains.compose.resources.stringResource


@Composable
fun ColumnScope.LocalNetworkConnection(backEntry: NavBackStackEntry) {
    Box(
        modifier = homeScreenCardModifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.local_network_connection_title),
                style = LocalAppTypography.current.titleMedium
            )
            SpacerHeight(5.dp)
            Text(
                text = stringResource(Res.string.local_network_connection_description),
                style = LocalAppTypography.current.bodyMedium
            )

            SpacerHeight(12.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {

                @Composable
                fun ActionButton(text: String, onClick: () -> Unit) {
                    Button(
                        modifier = Modifier
                            .height(50.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(width = 1.dp, color = LocalAppColorScheme.current.primary),
                        colors = ButtonDefaults.buttonColors().copy(
                            containerColor = LocalAppColorScheme.current.surface,
                            contentColor = LocalAppColorScheme.current.primary
                        ),
                        onClick = onClick,
                    ) {
                        Text(
                            text = text,
                            // color = LocalAppColorScheme.current.primary,
                            style = LocalAppTypography.current.titleMedium
                        )
                    }
                }

                // Scan QR Code
                ActionButton(text = stringResource(Res.string.local_network_scan_qr_code)) {
                    // TODO: Scan QR Code.
                }
                SpacerHeight(12.dp)
                // Display QR Code
                ActionButton(text = stringResource(Res.string.local_network_show_qr_code)) {
                    // TODO: Show QR Code.
                }
                SpacerHeight(12.dp)
                // Search servers
                ActionButton(text = stringResource(Res.string.broadcast_connection_as_sender)) {
                    // TODO: Search servers
                }
                SpacerHeight(12.dp)
                // As Server
                ActionButton(text = stringResource(Res.string.broadcast_connection_as_receiver)) {
                    // TODO: As server
                }
                SpacerHeight(8.dp)
            }
        }
    }
}