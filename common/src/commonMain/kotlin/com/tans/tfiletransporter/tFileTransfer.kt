package com.tans.tfiletransporter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
val LocalAppColorScheme = staticCompositionLocalOf<ColorScheme> {
    error("No app color scheme")
}
val LocalAppTypography = staticCompositionLocalOf<Typography> {
    error("No local app typography")
}



private val appLightColorTheme = lightColorScheme(
    // 主要颜色
    primary = AppColor.Teal200,
    onPrimary = AppColor.Black,
    primaryContainer = AppColor.Teal50,
    onPrimaryContainer = AppColor.Teal900,

    // 次要颜色
    secondary = AppColor.Red700,
    onSecondary = AppColor.White,
    secondaryContainer = AppColor.Red50,
    onSecondaryContainer = AppColor.Red900,

    // 表面和背景
    surface = AppColor.White,
    onSurface = AppColor.Black,
    surfaceVariant = AppColor.Gray100,
    onSurfaceVariant = AppColor.Gray800,
    background = AppColor.Gray50,
    onBackground = AppColor.Black,

    // 错误颜色
    error = AppColor.Red700,
    onError = AppColor.White,
    errorContainer = AppColor.Red100,
    onErrorContainer = AppColor.Red900,

    // 轮廓
    outline = AppColor.Gray400,
    outlineVariant = AppColor.Gray200
)

private val appDarkColorScheme = darkColorScheme(
    // 主要颜色
    primary = AppColor.Teal200,
    onPrimary = AppColor.Black,
    primaryContainer = AppColor.Teal900,
    onPrimaryContainer = AppColor.Teal100,

    // 次要颜色
    secondary = AppColor.Red300,
    onSecondary = AppColor.Black,
    secondaryContainer = AppColor.Red900,
    onSecondaryContainer = AppColor.Red100,

    // 表面和背景
    surface = AppColor.Gray900,
    onSurface = AppColor.White,
    surfaceVariant = AppColor.Gray800,
    onSurfaceVariant = AppColor.Gray200,
    background = AppColor.Black,
    onBackground = AppColor.White,

    // 错误颜色
    error = AppColor.Red300,
    onError = AppColor.Black,
    errorContainer = AppColor.Red800,
    onErrorContainer = AppColor.Red100,

    // 轮廓
    outline = AppColor.Gray600,
    outlineVariant = AppColor.Gray700
)

@Suppress("ComposableNaming")
@Composable
fun tFileTransfer() {
    val isInDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (!isInDarkTheme) appLightColorTheme else appDarkColorScheme
    val typography = MaterialTheme.typography
    AppTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        val navHostController = rememberNavController()
        CompositionLocalProvider(
            LocalNavHostController provides navHostController,
            LocalAppColorScheme provides colorScheme,
            LocalAppTypography provides typography
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
fun AppTheme(
    colorScheme: ColorScheme,
    typography: Typography,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}