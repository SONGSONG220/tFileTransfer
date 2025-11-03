package com.tans.tfiletransporter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tans.tfiletransporter.ui.home.HomeScreen


val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No navigation host controller")
}

@Suppress("ComposableNaming")
@Composable
fun tFileTransfer() {
    tFileTransferTheme {
        val navHostController = rememberNavController()
        CompositionLocalProvider(
            LocalNavHostController provides navHostController
        ) {
            NavHost(
                navController = navHostController,
                modifier = Modifier.fillMaxSize(),
                startDestination = Page.HomePage("Test url"),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                }
            ) {

                composable<Page.HomePage> {
                    HomeScreen(it)
                }
            }
        }
    }
}

@Suppress("ComposableNaming")
@Composable
fun tFileTransferTheme(content: @Composable () -> Unit) {
    // TODO: set colors.
    MaterialTheme (content = content)
}