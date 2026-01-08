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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

object ExpressiveMotion {
    // fast snappy animations - feels responsive
    val JumpySpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = 600f
    )
    
    // quick micro feedback
    val MicroBounce: AnimationSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )
    
    // instant snap for press feedback
    val QuickSnap: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1000f
    )
    
    // smooth but fast for progress
    val SmoothProgress: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
    
    // card elevation changes
    val CardElevation: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 600f
    )
    
    // expand/collapse sections - fast but smooth
    val SectionExpand: AnimationSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = 500f
    )
    
    fun staggeredDelay(index: Int, baseDelay: Int = 30) = index * baseDelay
    
    fun <T> staggeredSpec(index: Int): AnimationSpec<T> = tween(
        durationMillis = 200,
        delayMillis = staggeredDelay(index)
    )
}

object ExpressiveDurations {
    const val INSTANT = 50
    const val FAST = 100
    const val MEDIUM = 150
    const val SLOW = 250
    const val ENTRANCE = 200
    const val EXIT = 150
}

@Composable
fun rememberFloatingAnimation(
    initialValue: Float = 0f,
    targetValue: Float = 6f,
    durationMillis: Int = 1500
): Float {
    val transition = rememberInfiniteTransition(label = "float")
    val offset by transition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    return offset
}

@Composable
fun rememberPulseAnimation(
    minScale: Float = 0.97f,
    maxScale: Float = 1.03f,
    durationMillis: Int = 1000
): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    return scale
}
