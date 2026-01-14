package com.medownloader.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medownloader.R
import com.medownloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PaywallScreen(
    priceFormatted: String,
    isPurchasing: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    triggerReason: PaywallTriggerReason,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = gradientOffset * 200f
                )
            )
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.paywall_close_content_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated Pro Badge
            AnimatedProBadge()
            
            // Title based on trigger reason
            PaywallHeader(triggerReason = triggerReason)
            
            // Features list
            FeaturesSection()
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Error message
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = ExpressiveShapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Price and CTA
            PurchaseSection(
                formattedPrice = priceFormatted,
                isPurchasing = isPurchasing,
                onPurchaseClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onPurchase()
                }
            )
            
            // Restore purchases
            TextButton(
                onClick = onRestore,
                enabled = !isPurchasing
            ) {
                Text(
                    text = stringResource(R.string.paywall_restore),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Legal text
            Text(
                text = stringResource(R.string.paywall_legal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimatedProBadge() {
    // Slow-animating progress for gentle morphing effect
    val infiniteTransition = rememberInfiniteTransition(label = "slowMorph")
    val slowProgress by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )
    
    val floatOffset = rememberFloatingAnimation(targetValue = 8f, durationMillis = 4000)
    val pulseScale = rememberPulseAnimation(minScale = 0.97f, maxScale = 1.03f, durationMillis = 3000)
    
    Box(
        modifier = Modifier
            .offset(y = (-floatOffset).dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow with M3E wavy indicator (slow)
        CircularWavyProgressIndicator(
            progress = { slowProgress * 0.9f },
            modifier = Modifier.size(140.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
        
        // Inner M3E wavy indicator (slow)
        CircularWavyProgressIndicator(
            progress = { slowProgress },
            modifier = Modifier.size(100.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
        
        // Icon directly on top
        Icon(
            Icons.Filled.Bolt,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PaywallHeader(triggerReason: PaywallTriggerReason) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when (triggerReason) {
                PaywallTriggerReason.SPEED_BOOST,
                PaywallTriggerReason.SPEED_BOOST_BLOCKED -> stringResource(R.string.paywall_head_speed)
                PaywallTriggerReason.PREMIUM_THEME,
                PaywallTriggerReason.CUSTOM_THEMES_BLOCKED -> stringResource(R.string.paywall_head_themes)
                PaywallTriggerReason.TORRENT_BLOCKED -> stringResource(R.string.paywall_head_torrent)
                PaywallTriggerReason.CONCURRENT_LIMIT -> stringResource(R.string.paywall_head_concurrent)
                PaywallTriggerReason.SETTINGS,
                PaywallTriggerReason.SETTINGS_UPGRADE -> stringResource(R.string.paywall_head_settings)
                PaywallTriggerReason.MANUAL -> stringResource(R.string.paywall_head_manual)
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = when (triggerReason) {
                PaywallTriggerReason.SPEED_BOOST,
                PaywallTriggerReason.SPEED_BOOST_BLOCKED -> stringResource(R.string.paywall_sub_speed)
                PaywallTriggerReason.PREMIUM_THEME,
                PaywallTriggerReason.CUSTOM_THEMES_BLOCKED -> stringResource(R.string.paywall_sub_themes)
                PaywallTriggerReason.TORRENT_BLOCKED -> stringResource(R.string.paywall_sub_torrent)
                PaywallTriggerReason.CONCURRENT_LIMIT -> stringResource(R.string.paywall_sub_concurrent)
                PaywallTriggerReason.SETTINGS,
                PaywallTriggerReason.SETTINGS_UPGRADE -> stringResource(R.string.paywall_sub_settings)
                PaywallTriggerReason.MANUAL -> stringResource(R.string.paywall_sub_manual)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeaturesSection() {
    val features = listOf(
        Feature(
            icon = Icons.Outlined.Bolt,
            title = stringResource(R.string.paywall_feat_speed),
            description = stringResource(R.string.paywall_feat_speed_desc)
        ),
        Feature(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.paywall_feat_themes),
            description = stringResource(R.string.paywall_feat_themes_desc)
        ),
        Feature(
            icon = Icons.Outlined.AllInclusive,
            title = stringResource(R.string.paywall_feat_lifetime),
            description = stringResource(R.string.paywall_feat_lifetime_desc)
        ),
        Feature(
            icon = Icons.Outlined.Favorite,
            title = stringResource(R.string.paywall_feat_support),
            description = stringResource(R.string.paywall_feat_support_desc)
        )
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.forEachIndexed { index, feature ->
            FeatureRow(
                feature = feature,
                delay = index * 100
            )
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature, delay: Int) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.7f),
                    initialOffsetX = { -it / 2 }
                )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = ExpressiveShapeTokens.CardSoft
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = ExpressiveShapes.medium
                ) {
                    Icon(
                        feature.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PurchaseSection(
    formattedPrice: String,
    isPurchasing: Boolean,
    onPurchaseClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveMotion.Bouncy,
        label = "purchaseScale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Price display
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formattedPrice,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.paywall_lifetime),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Purchase button
        Button(
            onClick = onPurchaseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .scale(scale),
            enabled = !isPurchasing,
            interactionSource = interactionSource,
            // M3 Expressive: Use shapes parameter for morphing shape support
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.paywall_processing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.paywall_unlock_pro),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Data class
private data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * Reason for showing the paywall.
 * Affects the header text and messaging.
 */
enum class PaywallTriggerReason {
    SPEED_BOOST,           // User tried to increase connections (speed boost)
    SPEED_BOOST_BLOCKED,   // Speed boost blocked for free users
    PREMIUM_THEME,         // User tried to select premium theme
    CUSTOM_THEMES_BLOCKED, // Custom themes blocked
    SETTINGS,              // User accessed locked setting
    SETTINGS_UPGRADE,      // Upgrade from settings
    TORRENT_BLOCKED,       // Torrent/magnet blocked for free users
    CONCURRENT_LIMIT,      // Concurrent download limit reached
    MANUAL                 // User tapped upgrade button
}
