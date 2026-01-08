package com.medownloader.ui.theme

import androidx.compose.ui.graphics.Color

val PastelViolet = Color(0xFFB388FF)
val PastelVioletLight = Color(0xFFE1BEE7)
val PastelVioletDark = Color(0xFF7C4DFF)
val PastelVioletContainer = Color(0xFFF3E5F5)

val PastelCyan = Color(0xFF84FFFF)
val PastelCyanDark = Color(0xFF18FFFF)
val PastelMint = Color(0xFFA7FFEB)
val PastelPeach = Color(0xFFFFCCBC)
val PastelPink = Color(0xFFF8BBD0)
val PastelLime = Color(0xFFF0F4C3)

val SurfaceLight = Color(0xFFFFFBFE)
val SurfaceDim = Color(0xFFF5F0F7)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFFAF5FC)
val SurfaceContainer = Color(0xFFF5EFF7)
val SurfaceContainerHigh = Color(0xFFEFE9F2)
val SurfaceContainerHighest = Color(0xFFE9E4EC)

val DarkSurface = Color(0xFF1C1B1F)
val DarkSurfaceDim = Color(0xFF141218)
val DarkSurfaceContainer = Color(0xFF211F26)
val DarkSurfaceContainerHigh = Color(0xFF2B2930)
val DarkSurfaceContainerHighest = Color(0xFF36343B)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedLight = Color(0xFFFFB4AB)
val ErrorContainer = Color(0xFFFFDAD6)
val ErrorContainerDark = Color(0xFF93000A)

val SuccessGreen = Color(0xFF4CAF50)
val SuccessContainer = Color(0xFFD7F5D7)
val SuccessContainerDark = Color(0xFF1B5E20)

val WarningAmber = Color(0xFFFFA000)
val WarningContainer = Color(0xFFFFE082)

sealed class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color
) {
    data object VioletDream : ThemePalette(
        primary = Color(0xFF7C4DFF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8DEF8),
        onPrimaryContainer = Color(0xFF21005E),
        secondary = Color(0xFFCE93D8),
        onSecondary = Color(0xFF3D2845),
        secondaryContainer = Color(0xFFF3E5F5),
        onSecondaryContainer = Color(0xFF3D2845),
        tertiary = Color(0xFF84FFFF),
        onTertiary = Color(0xFF003737),
        tertiaryContainer = Color(0xFFE0F7FA),
        onTertiaryContainer = Color(0xFF003737),
        surface = SurfaceLight,
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0)
    )
    
    data object VioletDreamDark : ThemePalette(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFF84FFFF),
        onTertiary = Color(0xFF003737),
        tertiaryContainer = Color(0xFF004F50),
        onTertiaryContainer = Color(0xFFA7FFEB),
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F)
    )
    
    data object CatppuccinMocha : ThemePalette(
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
        surface = Color(0xFF1E1E2E),
        onSurface = Color(0xFFCDD6F4),
        surfaceVariant = Color(0xFF302D41),
        onSurfaceVariant = Color(0xFFBAC2DE),
        outline = Color(0xFF6C7086),
        outlineVariant = Color(0xFF45475A)
    )

    data object GruvboxDark : ThemePalette(
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
        surface = Color(0xFF282828),
        onSurface = Color(0xFFEBDBB2),
        surfaceVariant = Color(0xFF3C3836),
        onSurfaceVariant = Color(0xFFD5C4A1),
        outline = Color(0xFF7C6F64),
        outlineVariant = Color(0xFF504945)
    )

    data object TokyoNight : ThemePalette(
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
        surface = Color(0xFF1A1B26),
        onSurface = Color(0xFFC0CAF5),
        surfaceVariant = Color(0xFF24283B),
        onSurfaceVariant = Color(0xFFA9B1D6),
        outline = Color(0xFF565F89),
        outlineVariant = Color(0xFF3B4261)
    )

    data object Nord : ThemePalette(
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
        surface = Color(0xFF2E3440),
        onSurface = Color(0xFFECEFF4),
        surfaceVariant = Color(0xFF3B4252),
        onSurfaceVariant = Color(0xFFD8DEE9),
        outline = Color(0xFF4C566A),
        outlineVariant = Color(0xFF3B4252)
    )
}
