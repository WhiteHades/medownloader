package com.medownloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shape System
 * 
 * 35 new shape tokens with dynamic morphing support.
 * Personality-driven geometric language.
 * Full corners for highly interactive elements (FAB, buttons).
 */

val ExpressiveShapes = Shapes(
    // ExtraSmall: Chips, badges, small interactive elements
    extraSmall = RoundedCornerShape(6.dp),
    
    // Small: Buttons, text fields, snackbars
    small = RoundedCornerShape(12.dp),
    
    // Medium: Cards, list items, medium containers
    medium = RoundedCornerShape(16.dp),
    
    // Large: Bottom sheets (top corners), large cards
    large = RoundedCornerShape(24.dp),
    
    // ExtraLarge: Full screen dialogs, hero containers
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Additional Expressive Shape Tokens
 */
object ExpressiveShapeTokens {
    // Fully rounded (FAB, pills, circular buttons)
    val Full = RoundedCornerShape(percent = 50)
    
    // Top only (bottom sheets)
    val TopLarge = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val TopExtraLarge = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    
    // Cards with different personalities
    val CardSoft = RoundedCornerShape(20.dp)
    val CardBold = RoundedCornerShape(24.dp)
    val CardHero = RoundedCornerShape(28.dp)
    
    // Progress indicators
    val ProgressTrack = RoundedCornerShape(8.dp)
    val ProgressIndicator = RoundedCornerShape(8.dp)
    
    // Dialogs
    val Dialog = RoundedCornerShape(28.dp)
    val AlertDialog = RoundedCornerShape(24.dp)
    
    // Floating toolbar (pill-shaped)
    val FloatingToolbar = RoundedCornerShape(percent = 50)
    
    // Stats cards
    val StatsCard = RoundedCornerShape(18.dp)
    
    // Input fields
    val TextField = RoundedCornerShape(14.dp)
    
    // Navigation
    val NavIndicator = RoundedCornerShape(percent = 50)
    
    // Bottom sheets
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    
    // Buttons
    val ButtonLarge = RoundedCornerShape(16.dp)
    val ButtonPill = RoundedCornerShape(percent = 50)
}

// Legacy compatibility
val CardShape = ExpressiveShapeTokens.CardSoft
val Shapes = ExpressiveShapes
