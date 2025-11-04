package com.tans.tfiletransporter.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.tans.tfiletransporter.resources.Res
import com.tans.tfiletransporter.resources.app_name
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(backEntry: NavBackStackEntry) {
    // val homePage = backEntry.toRoute<Page.HomePage>()
    val store: HomeStateStore = viewModel<HomeStateStore>(backEntry) {
        HomeStateStore()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(2.dp),
                title = {
                    Text(text = stringResource(Res.string.app_name))
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it)) {

        }
    }
}