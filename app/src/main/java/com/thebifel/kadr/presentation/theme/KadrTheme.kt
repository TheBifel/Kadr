package com.thebifel.kadr.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat

internal val Accent: Color
    @Composable get() = MaterialTheme.colorScheme.primary
internal val Bg: Color
    @Composable get() = MaterialTheme.colorScheme.background
internal val Border: Color
    @Composable get() = MaterialTheme.colorScheme.outline
internal val Muted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val Ink: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@Composable internal fun S(id: Int, vararg args: Any) = stringResource(id, *args)

@Composable
fun KadrTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette =
        if (dark)
            darkColorScheme(
                primary = Color(0xFFD2F680),
                onPrimary = Color(0xFF203014),
                background = Color(0xFF101410),
                surface = Color(0xFF191F17),
                surfaceVariant = Color(0xFF1C2419),
                onSurface = Color(0xFFEDF1E5),
                onBackground = Color(0xFFEDF1E5),
                onSurfaceVariant = Color(0xFF9DAA91),
                outline = Color(0xFF36412F),
                primaryContainer = Color(0xFF242F1B),
                onPrimaryContainer = Color(0xFFB8CBA3),
                errorContainer = Color(0xFF34291D),
                onErrorContainer = Color(0xFFE4BA91),
            )
        else
            lightColorScheme(
                primary = Color(0xFF4C6B24),
                onPrimary = Color.White,
                background = Color(0xFFF4F5EE),
                surface = Color(0xFFEEF1E5),
                surfaceVariant = Color(0xFFE4E9DB),
                onSurface = Color(0xFF202A1A),
                onBackground = Color(0xFF202A1A),
                onSurfaceVariant = Color(0xFF657157),
                outline = Color(0xFFC7D1BB),
                primaryContainer = Color(0xFFE1EDCE),
                onPrimaryContainer = Color(0xFF344B1E),
                errorContainer = Color(0xFFFFE9D6),
                onErrorContainer = Color(0xFF77471F),
            )
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = palette, content = content)
}
