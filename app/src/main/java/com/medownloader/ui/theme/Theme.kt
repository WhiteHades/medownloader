package com.medownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
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

/**
 * App Theme Selection
 */
enum class AppTheme {
    DEFAULT,
    CATPPUCCIN,
    TOKYO_NIGHT,
    GRUVBOX,
    NORD
}

// ============================================================
// DEFAULT THEME: Electric Blue + Cyber Violet + Neon Cyan
// ============================================================

private val DefaultLightScheme = lightColorScheme(
    primary = ElectricBlue50,
    onPrimary = Color.White,
    primaryContainer = ElectricBlue90,
    onPrimaryContainer = ElectricBlue10,
    inversePrimary = ElectricBlue80,
    
    secondary = CyberViolet50,
    onSecondary = Color.White,
    secondaryContainer = CyberViolet90,
    onSecondaryContainer = CyberViolet10,
    
    tertiary = NeonCyan50,
    onTertiary = Color.White,
    tertiaryContainer = NeonCyan90,
    onTertiaryContainer = NeonCyan10,
    
    error = ErrorRed40,
    onError = Color.White,
    errorContainer = ErrorRed90,
    onErrorContainer = ErrorRed10,
    
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    
    surfaceTint = ElectricBlue50,
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    scrim = Color.Black,
    
    surfaceBright = Color(0xFFFDFBFF),
    surfaceDim = Color(0xFFDCD9DE),
    surfaceContainer = Color(0xFFF1EFF4),
    surfaceContainerHigh = Color(0xFFEBE9EE),
    surfaceContainerHighest = Color(0xFFE5E3E8),
    surfaceContainerLow = Color(0xFFF7F5FA),
    surfaceContainerLowest = Color.White
)

private val DefaultDarkScheme = darkColorScheme(
    primary = ElectricBlue80,
    onPrimary = ElectricBlue20,
    primaryContainer = ElectricBlue30,
    onPrimaryContainer = ElectricBlue90,
    inversePrimary = ElectricBlue50,
    
    secondary = CyberViolet80,
    onSecondary = CyberViolet20,
    secondaryContainer = CyberViolet30,
    onSecondaryContainer = CyberViolet90,
    
    tertiary = NeonCyan80,
    onTertiary = NeonCyan20,
    tertiaryContainer = NeonCyan30,
    onTertiaryContainer = NeonCyan90,
    
    error = ErrorRed80,
    onError = ErrorRed20,
    errorContainer = ErrorRed30,
    onErrorContainer = ErrorRed90,
    
    background = Color(0xFF111113),
    onBackground = Color(0xFFE3E2E6),
    
    surface = Color(0xFF111113),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    
    surfaceTint = ElectricBlue80,
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    scrim = Color.Black,
    
    surfaceBright = Color(0xFF39393C),
    surfaceDim = Color(0xFF111113),
    surfaceContainer = Color(0xFF1D1D20),
    surfaceContainerHigh = Color(0xFF27272A),
    surfaceContainerHighest = Color(0xFF323235),
    surfaceContainerLow = Color(0xFF1A1A1C),
    surfaceContainerLowest = Color(0xFF0D0D0F)
)

// ============================================================
// CATPPUCCIN MOCHA (Premium)
// ============================================================

private val CatppuccinDarkScheme = darkColorScheme(
    primary = CatppuccinMocha.pink,
    onPrimary = CatppuccinMocha.crust,
    primaryContainer = CatppuccinMocha.surface1,
    onPrimaryContainer = CatppuccinMocha.pink,
    
    secondary = CatppuccinMocha.green,
    onSecondary = CatppuccinMocha.crust,
    secondaryContainer = CatppuccinMocha.surface1,
    onSecondaryContainer = CatppuccinMocha.green,
    
    tertiary = CatppuccinMocha.sky,
    onTertiary = CatppuccinMocha.crust,
    tertiaryContainer = CatppuccinMocha.surface1,
    onTertiaryContainer = CatppuccinMocha.sky,
    
    error = CatppuccinMocha.red,
    onError = CatppuccinMocha.crust,
    errorContainer = CatppuccinMocha.surface1,
    onErrorContainer = CatppuccinMocha.red,
    
    background = CatppuccinMocha.base,
    onBackground = CatppuccinMocha.text,
    
    surface = CatppuccinMocha.base,
    onSurface = CatppuccinMocha.text,
    surfaceVariant = CatppuccinMocha.surface0,
    onSurfaceVariant = CatppuccinMocha.subtext1,
    
    outline = CatppuccinMocha.overlay1,
    outlineVariant = CatppuccinMocha.surface1,
    
    surfaceContainer = CatppuccinMocha.surface0,
    surfaceContainerHigh = CatppuccinMocha.surface1,
    surfaceContainerHighest = CatppuccinMocha.surface2,
    surfaceContainerLow = CatppuccinMocha.mantle,
    surfaceContainerLowest = CatppuccinMocha.crust
)

// ============================================================
// TOKYO NIGHT (Premium)
// ============================================================

private val TokyoNightDarkScheme = darkColorScheme(
    primary = TokyoNight.blue,
    onPrimary = TokyoNight.bg_dark,
    primaryContainer = TokyoNight.bg_highlight,
    onPrimaryContainer = TokyoNight.blue,
    
    secondary = TokyoNight.green,
    onSecondary = TokyoNight.bg_dark,
    secondaryContainer = TokyoNight.bg_highlight,
    onSecondaryContainer = TokyoNight.green,
    
    tertiary = TokyoNight.magenta,
    onTertiary = TokyoNight.bg_dark,
    tertiaryContainer = TokyoNight.bg_highlight,
    onTertiaryContainer = TokyoNight.magenta,
    
    error = TokyoNight.red,
    onError = TokyoNight.bg_dark,
    
    background = TokyoNight.bg,
    onBackground = TokyoNight.fg,
    
    surface = TokyoNight.bg,
    onSurface = TokyoNight.fg,
    surfaceVariant = TokyoNight.bg_highlight,
    onSurfaceVariant = TokyoNight.fg_dark,
    
    outline = TokyoNight.fg_dark,
    
    surfaceContainer = TokyoNight.bg_dark,
    surfaceContainerHigh = TokyoNight.bg_highlight,
    surfaceContainerLow = Color(0xFF13141C)
)

// ============================================================
// GRUVBOX (Premium)
// ============================================================

private val GruvboxDarkScheme = darkColorScheme(
    primary = GruvboxDark.orange,
    onPrimary = GruvboxDark.bg,
    primaryContainer = GruvboxDark.bg2,
    onPrimaryContainer = GruvboxDark.orange,
    
    secondary = GruvboxDark.aqua,
    onSecondary = GruvboxDark.bg,
    secondaryContainer = GruvboxDark.bg2,
    onSecondaryContainer = GruvboxDark.aqua,
    
    tertiary = GruvboxDark.yellow,
    onTertiary = GruvboxDark.bg,
    tertiaryContainer = GruvboxDark.bg2,
    onTertiaryContainer = GruvboxDark.yellow,
    
    error = GruvboxDark.red,
    onError = GruvboxDark.bg,
    
    background = GruvboxDark.bg,
    onBackground = GruvboxDark.fg,
    
    surface = GruvboxDark.bg,
    onSurface = GruvboxDark.fg,
    surfaceVariant = GruvboxDark.bg1,
    onSurfaceVariant = GruvboxDark.fg,
    
    outline = GruvboxDark.bg2,
    
    surfaceContainer = GruvboxDark.bg1,
    surfaceContainerHigh = GruvboxDark.bg2,
    surfaceContainerLow = Color(0xFF1D2021)
)

// ============================================================
// NORD (Premium)
// ============================================================

private val NordDarkScheme = darkColorScheme(
    primary = Nord.frost1,
    onPrimary = Nord.polarNight0,
    primaryContainer = Nord.polarNight2,
    onPrimaryContainer = Nord.frost1,
    
    secondary = Nord.frost2,
    onSecondary = Nord.polarNight0,
    secondaryContainer = Nord.polarNight2,
    onSecondaryContainer = Nord.frost2,
    
    tertiary = Nord.auroraPurple,
    onTertiary = Nord.polarNight0,
    tertiaryContainer = Nord.polarNight2,
    onTertiaryContainer = Nord.auroraPurple,
    
    error = Nord.auroraRed,
    onError = Nord.polarNight0,
    
    background = Nord.polarNight0,
    onBackground = Nord.snowStorm2,
    
    surface = Nord.polarNight0,
    onSurface = Nord.snowStorm2,
    surfaceVariant = Nord.polarNight1,
    onSurfaceVariant = Nord.snowStorm0,
    
    outline = Nord.polarNight2,
    
    surfaceContainer = Nord.polarNight1,
    surfaceContainerHigh = Nord.polarNight2,
    surfaceContainerLow = Color(0xFF242933)
)

// ============================================================
// THEME COMPOSABLE (Material 3 Expressive 2025)
// ============================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.DEFAULT,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color on Android 12+ (only for DEFAULT theme)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appTheme == AppTheme.DEFAULT -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // Premium themes (dark only for expressive feel)
        appTheme == AppTheme.CATPPUCCIN -> CatppuccinDarkScheme
        appTheme == AppTheme.TOKYO_NIGHT -> TokyoNightDarkScheme
        appTheme == AppTheme.GRUVBOX -> GruvboxDarkScheme
        appTheme == AppTheme.NORD -> NordDarkScheme
        
        // Default theme
        darkTheme -> DefaultDarkScheme
        else -> DefaultLightScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme && appTheme == AppTheme.DEFAULT
                isAppearanceLightNavigationBars = !darkTheme && appTheme == AppTheme.DEFAULT
            }
        }
    }

    // Use MaterialExpressiveTheme for full M3 Expressive support
    // This enables morphing shapes, expressive motion, and new component APIs
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        motionScheme = ExpressiveMotionScheme,
        content = content
    )
}
