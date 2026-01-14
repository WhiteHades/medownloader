package com.medownloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object ExpressiveShapeTokens {
    val Full = RoundedCornerShape(percent = 50)
    val TopLarge = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val TopExtraLarge = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val FabBlob = RoundedCornerShape(
        topStart = 30.dp,
        topEnd = 22.dp,
        bottomEnd = 30.dp,
        bottomStart = 22.dp
    )
    val FabDiamond = CutCornerShape(16.dp)
    val FabSquircle = RoundedCornerShape(26.dp)
    val CardSoft = RoundedCornerShape(20.dp)
    val CardBold = RoundedCornerShape(24.dp)
    val CardHero = RoundedCornerShape(28.dp)
    val CardTilted = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomEnd = 24.dp,
        bottomStart = 8.dp
    )
    val ProgressTrack = RoundedCornerShape(8.dp)
    val ProgressIndicator = RoundedCornerShape(8.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val AlertDialog = RoundedCornerShape(24.dp)
    val FloatingToolbar = RoundedCornerShape(percent = 50)
    val StatsCard = RoundedCornerShape(18.dp)
    val StatsCardAlt = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 12.dp,
        bottomEnd = 20.dp,
        bottomStart = 12.dp
    )
    val TextField = RoundedCornerShape(14.dp)
    val NavIndicator = RoundedCornerShape(percent = 50)
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val ButtonLarge = RoundedCornerShape(16.dp)
    val ButtonPill = RoundedCornerShape(percent = 50)
    val FileIcon = RoundedCornerShape(12.dp)
}

val CardShape = ExpressiveShapeTokens.CardSoft
val Shapes = ExpressiveShapes
