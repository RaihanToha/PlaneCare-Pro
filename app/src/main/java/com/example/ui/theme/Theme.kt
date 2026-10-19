package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenPrimaryDark,
    secondary = SlateSecondaryDark,
    tertiary = TerracottaTertiaryDark,
    background = DeepCharcoalBackground,
    surface = DarkSlateSurface,
    onPrimary = DeepCharcoalBackground,
    onSecondary = DeepCharcoalBackground,
    onTertiary = DeepCharcoalBackground,
    onBackground = SageBackground,
    onSurface = SageBackground
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    secondary = SlateSecondary,
    tertiary = TerracottaTertiary,
    background = SageBackground,
    surface = MintSurface,
    onPrimary = MintSurface,
    onSecondary = MintSurface,
    onTertiary = MintSurface,
    onBackground = DeepCharcoalBackground,
    onSurface = DeepCharcoalBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false by default for our design so we strictly use our beautiful Green/Slate palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
