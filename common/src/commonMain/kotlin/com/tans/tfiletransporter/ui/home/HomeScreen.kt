package com.tans.tfiletransporter.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.tans.tfiletransporter.LocalAppColorScheme
import com.tans.tfiletransporter.Platform
import com.tans.tfiletransporter.platform
import com.tans.tfiletransporter.resources.Res
import com.tans.tfiletransporter.resources.app_name
import com.tans.tfiletransporter.resources.ic_settings
import com.tans.tfiletransporter.ui.SpacerHeight
import com.tans.tfiletransporter.ui.home.localnetwork.LocalNetworkConnection
import com.tans.tfiletransporter.ui.home.wifidirect.WiFiDirectConnection
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(backEntry: NavBackStackEntry) {
    // val homePage = backEntry.toRoute<Page.HomePage>()
    val store: HomeStateStore = viewModel<HomeStateStore>(backEntry) {
        HomeStateStore()
    }
    val scrollableState = rememberScrollState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(2.dp),
                title = {
                    Text(text = stringResource(Res.string.app_name))
                },
                actions = {
                    IconButton(onClick = {
                        // TODO: Show settings dialog.
                    }) {
                        Image(painter = painterResource(Res.drawable.ic_settings), contentDescription = null)
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = LocalAppColorScheme.current.surfaceVariant)
                .padding(it)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .scrollable(
                        state = scrollableState,
                        orientation = Orientation.Vertical
                    )
                    .padding(top = 14.dp, bottom = 14.dp, start = 12.dp, end = 12.dp)
            ) {

                WiFiDirectConnection(backEntry)

                if (platform() == Platform.Android) {
                    SpacerHeight(12.dp)
                }

                LocalNetworkConnection(backEntry)
            }
        }
    }
}

@get:Composable
inline val ColumnScope.homeScreenCardModifier
    get() = Modifier
        .widthIn(max = 700.dp)
        .fillMaxWidth()
        .shadow(1.dp, clip = true, shape = RoundedCornerShape(8.dp))
        .background(LocalAppColorScheme.current.surface, RoundedCornerShape(8.dp))
        .padding(10.dp)
        .align(Alignment.CenterHorizontally)