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

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalPrimaryContainer,
    onPrimary = NaturalTextDark,
    primaryContainer = NaturalPrimaryDarkGreen,
    onPrimaryContainer = NaturalBg,
    secondary = NaturalSecondaryContainer,
    onSecondary = NaturalTextDark,
    secondaryContainer = NaturalSecondary,
    onSecondaryContainer = NaturalBg,
    background = NaturalTextDark,
    onBackground = NaturalBg,
    surface = NaturalSecondary,
    onSurface = NaturalBg,
    surfaceVariant = NaturalSecondary,
    onSurfaceVariant = NaturalBg,
    outline = NaturalOutline,
    error = NaturalErrorRed,
    onError = NaturalOnError,
    errorContainer = NaturalErrorContainer,
    onErrorContainer = NaturalOnErrorContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalPrimaryDarkGreen,
    onPrimary = NaturalPrimaryOn,
    primaryContainer = NaturalPrimaryContainer,
    onPrimaryContainer = NaturalOnPrimaryContainer,
    secondary = NaturalSecondary,
    onSecondary = NaturalPrimaryOn,
    secondaryContainer = NaturalSecondaryContainer,
    onSecondaryContainer = NaturalOnSecondaryContainer,
    background = NaturalBg,
    onBackground = NaturalTextDark,
    surface = NaturalSurface,
    onSurface = NaturalTextDark,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalOnSurfaceVariant,
    outline = NaturalOutline,
    error = NaturalErrorRed,
    onError = NaturalOnError,
    errorContainer = NaturalErrorContainer,
    onErrorContainer = NaturalOnErrorContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Default dynamicColor to false to preserve the customized Natural Tones brand theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
