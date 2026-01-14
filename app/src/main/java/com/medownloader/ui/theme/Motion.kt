package com.medownloader.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Material 3 Expressive Motion System (2025)
 * 
 * Uses official M3 Expressive MotionScheme with physics-based spring dynamics.
 * Two modes: Expressive (default) and Standard (reduced motion).
 * 
 * Defining characteristic: Motion feels organic, playful, and alive.
 */

/**
 * Official M3 Expressive Motion Scheme
 * Use with MaterialExpressiveTheme for full M3E compliance.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ExpressiveMotionScheme: MotionScheme = MotionScheme.expressive()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val StandardMotionScheme: MotionScheme = MotionScheme.standard()

/**
 * Custom Motion Specs for fine-grained control
 */
object ExpressiveMotion {
    
    // ============================================================
    // SPATIAL ANIMATIONS (Moving through space)
    // ============================================================
    
    /** Fast spatial: 200ms - FAB morphing, quick transitions */
    val SpatialFast: AnimationSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = 0.001f
    )
    
    /** Standard spatial: 300ms - Cards entering/exiting, navigation */
    val SpatialStandard: AnimationSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = 0.001f
    )
    
    /** Slow spatial: 400-500ms - Complex transitions, hero animations */
    val SpatialSlow: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessVeryLow,
        visibilityThreshold = 0.001f
    )
    
    // ============================================================
    // EFFECT ANIMATIONS (Emphasis and delight)
    // ============================================================
    
    /** Bouncy spring with overshoot - satisfying button press feedback */
    val Bouncy: AnimationSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = 600f
    )
    
    /** Micro bounce - quick confirmation haptic feel */
    val MicroBounce: AnimationSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )
    
    /** Confetti/celebration - burst effects */
    val Celebration: AnimationSpec<Float> = spring(
        dampingRatio = 0.4f,
        stiffness = 500f
    )
    
    // ============================================================
    // STATE TRANSITIONS (100-200ms morphing)
    // ============================================================
    
    /** Quick snap - instant press feedback */
    val QuickSnap: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1000f
    )
    
    /** State morph - checkbox, toggle animations */
    val StateMorph: AnimationSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = 700f
    )
    
    /** Card press - lift/elevation changes */
    val CardPress: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 600f
    )
    
    // ============================================================
    // SCROLL & PROGRESS ANIMATIONS
    // ============================================================
    
    /** Smooth progress - interpolated, not jumpy */
    val SmoothProgress: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
    
    /** Section expand/collapse */
    val SectionExpand: AnimationSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = 500f
    )
    
    /** Bottom sheet slide with overshoot */
    val BottomSheetSlide: AnimationSpec<Float> = spring(
        dampingRatio = 0.65f,
        stiffness = 400f
    )
    
    /** Dialog scale in with pop */
    val DialogPop: AnimationSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 500f
    )
    
    // ============================================================
    // STAGGERED ANIMATIONS
    // ============================================================
    
    fun staggeredDelay(index: Int, baseDelay: Int = 50) = index * baseDelay
    
    fun <T> staggeredSpec(index: Int): AnimationSpec<T> = tween(
        durationMillis = 200,
        delayMillis = staggeredDelay(index)
    )
}

/**
 * Expressive Duration Constants (milliseconds)
 */
object ExpressiveDurations {
    const val INSTANT = 50
    const val MICRO = 100
    const val FAST = 150
    const val STANDARD = 200
    const val MEDIUM = 300
    const val SLOW = 400
    const val ENTRANCE = 250
    const val EXIT = 200
}

/**
 * Floating animation for FAB/cards - creates gentle hover effect
 */
@Composable
fun rememberFloatingAnimation(
    initialValue: Float = 0f,
    targetValue: Float = 8f,
    durationMillis: Int = 2000
): Float {
    val transition = rememberInfiniteTransition(label = "float")
    val offset by transition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    return offset
}

/**
 * Pulse animation for active downloads
 */
@Composable
fun rememberPulseAnimation(
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f,
    durationMillis: Int = 1200
): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    return scale
}

/**
 * Glow/shimmer animation for premium elements
 */
@Composable
fun rememberShimmerAnimation(durationMillis: Int = 1500): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    return shimmer
}
