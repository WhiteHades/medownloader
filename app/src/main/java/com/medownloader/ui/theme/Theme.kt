package com.medownloader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme {
    DEFAULT,
    CATPPUCCIN,
    GRUVBOX,
    TOKYO_NIGHT,
    NORD
}

private val DefaultLightScheme = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF21005E),
    inversePrimary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF3D2845),
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF3D2845),
    tertiary = Color(0xFF84FFFF),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFFE0F7FA),
    onTertiaryContainer = Color(0xFF003737),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Color(0xFF7C4DFF),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFFFBFE),
    surfaceDim = Color(0xFFDED8DD),
    surfaceContainer = Color(0xFFF3EDF2),
    surfaceContainerHigh = Color(0xFFEDE7EC),
    surfaceContainerHighest = Color(0xFFE7E1E6),
    surfaceContainerLow = Color(0xFFF9F3F8),
    surfaceContainerLowest = Color.White
)

private val DefaultDarkScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    inversePrimary = Color(0xFF7C4DFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFF84FFFF),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF004F50),
    onTertiaryContainer = Color(0xFFA7FFEB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFFD0BCFF),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black,
    surfaceBright = Color(0xFF3B383E),
    surfaceDim = Color(0xFF1C1B1F),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerLowest = Color(0xFF0F0D13)
)

private val CatppuccinDarkScheme = darkColorScheme(
    primary = Color(0xFFF5C2E7),
    onPrimary = Color(0xFF302D41),
    primaryContainer = Color(0xFF575268),
    onPrimaryContainer = Color(0xFFFAE3FF),
    secondary = Color(0xFFABE9B3),
    onSecondary = Color(0xFF302D41),
    secondaryContainer = Color(0xFF4D5A56),
    onSecondaryContainer = Color(0xFFD7F5DC),
    tertiary = Color(0xFF89DCEB),
    onTertiary = Color(0xFF302D41),
    tertiaryContainer = Color(0xFF4D5B66),
    onTertiaryContainer = Color(0xFFD4F0FF),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF302D41),
    errorContainer = Color(0xFF8C3A4B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF302D41),
    onSurfaceVariant = Color(0xFFBAC2DE),
    outline = Color(0xFF6C7086),
    outlineVariant = Color(0xFF45475A),
    surfaceContainer = Color(0xFF313244),
    surfaceContainerHigh = Color(0xFF45475A),
    surfaceContainerHighest = Color(0xFF585B70),
    surfaceContainerLow = Color(0xFF24273A),
    surfaceContainerLowest = Color(0xFF181825)
)

private val GruvboxDarkScheme = darkColorScheme(
    primary = Color(0xFFFE8019),
    onPrimary = Color(0xFF282828),
    primaryContainer = Color(0xFF504945),
    onPrimaryContainer = Color(0xFFFFD6A5),
    secondary = Color(0xFF8EC07C),
    onSecondary = Color(0xFF282828),
    secondaryContainer = Color(0xFF504945),
    onSecondaryContainer = Color(0xFFD5EADD),
    tertiary = Color(0xFFFABD2F),
    onTertiary = Color(0xFF282828),
    tertiaryContainer = Color(0xFF504945),
    onTertiaryContainer = Color(0xFFFFF8DC),
    error = Color(0xFFFB4934),
    onError = Color(0xFF282828),
    errorContainer = Color(0xFF9D0006),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF282828),
    onBackground = Color(0xFFEBDBB2),
    surface = Color(0xFF282828),
    onSurface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFF3C3836),
    onSurfaceVariant = Color(0xFFD5C4A1),
    outline = Color(0xFF7C6F64),
    outlineVariant = Color(0xFF504945),
    surfaceContainer = Color(0xFF3C3836),
    surfaceContainerHigh = Color(0xFF504945),
    surfaceContainerHighest = Color(0xFF665C54),
    surfaceContainerLow = Color(0xFF32302F),
    surfaceContainerLowest = Color(0xFF1D2021)
)

private val TokyoNightDarkScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF1A1B26),
    primaryContainer = Color(0xFF3D4C6C),
    onPrimaryContainer = Color(0xFFC0D5FF),
    secondary = Color(0xFF9ECE6A),
    onSecondary = Color(0xFF1A1B26),
    secondaryContainer = Color(0xFF3C4D38),
    onSecondaryContainer = Color(0xFFD7F5C0),
    tertiary = Color(0xFFBB9AF7),
    onTertiary = Color(0xFF1A1B26),
    tertiaryContainer = Color(0xFF4D3D6C),
    onTertiaryContainer = Color(0xFFEDD5FF),
    error = Color(0xFFF7768E),
    onError = Color(0xFF1A1B26),
    errorContainer = Color(0xFF8C3A4B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF1A1B26),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF24283B),
    onSurfaceVariant = Color(0xFFA9B1D6),
    outline = Color(0xFF565F89),
    outlineVariant = Color(0xFF3B4261),
    surfaceContainer = Color(0xFF24283B),
    surfaceContainerHigh = Color(0xFF343A52),
    surfaceContainerHighest = Color(0xFF414868),
    surfaceContainerLow = Color(0xFF1F202E),
    surfaceContainerLowest = Color(0xFF16161E)
)

private val NordDarkScheme = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF2E3440),
    primaryContainer = Color(0xFF434C5E),
    onPrimaryContainer = Color(0xFFD1E8F0),
    secondary = Color(0xFF81A1C1),
    onSecondary = Color(0xFF2E3440),
    secondaryContainer = Color(0xFF434C5E),
    onSecondaryContainer = Color(0xFFD1DEF0),
    tertiary = Color(0xFFB48EAD),
    onTertiary = Color(0xFF2E3440),
    tertiaryContainer = Color(0xFF4C3A51),
    onTertiaryContainer = Color(0xFFEDD9E8),
    error = Color(0xFFBF616A),
    onError = Color(0xFF2E3440),
    errorContainer = Color(0xFF8C3A4B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF3B4252),
    onSurfaceVariant = Color(0xFFD8DEE9),
    outline = Color(0xFF4C566A),
    outlineVariant = Color(0xFF3B4252),
    surfaceContainer = Color(0xFF3B4252),
    surfaceContainerHigh = Color(0xFF434C5E),
    surfaceContainerHighest = Color(0xFF4C566A),
    surfaceContainerLow = Color(0xFF333A47),
    surfaceContainerLowest = Color(0xFF272D38)
)

@Composable
fun MeDownloaderTheme(
    theme: AppTheme = AppTheme.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (theme) {
            AppTheme.DEFAULT -> if (darkTheme) DefaultDarkScheme else DefaultLightScheme
            AppTheme.CATPPUCCIN -> CatppuccinDarkScheme
            AppTheme.GRUVBOX -> GruvboxDarkScheme
            AppTheme.TOKYO_NIGHT -> TokyoNightDarkScheme
            AppTheme.NORD -> NordDarkScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
