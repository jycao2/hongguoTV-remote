package com.hongguo.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedPrimary = Color(0xFFE1251B) // 红果品牌红
private val YellowSecondary = Color(0xFFF2A900)

private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    secondary = YellowSecondary,
    tertiary = Color(0xFFEF5350)
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = YellowSecondary,
    tertiary = Color(0xFFEF5350)
)

@Composable
fun HongguoRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
