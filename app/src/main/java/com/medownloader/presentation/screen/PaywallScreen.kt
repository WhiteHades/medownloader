package com.medownloader.presentation.screen

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType

/**
 * Paywall Screen - $3.99 Lifetime Only
 * 
 * Design Philosophy:
 * - ONE button, ONE price, ZERO confusion
 * - "Buy Once, Own Forever" - trust signal
 * - Feature comparison shows tangible value
 * - No subscription anxiety
 * 
 * Why Lifetime Only?
 * - Download managers are burst-usage tools
 * - Subscriptions cause churn (subscribe, download, cancel)
 * - $3.99 = "three dollars and change" (charm pricing)
 * - Users feel ownership, we keep the money
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    priceFormatted: String,
    isPurchasing: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    triggerReason: PaywallTriggerReason? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("unlock pro") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            if (triggerReason != null && triggerReason != PaywallTriggerReason.SETTINGS_UPGRADE) {
                TriggerReasonBanner(triggerReason)
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Hero Section
                PaywallHero()
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Feature Comparison Table
            FeatureComparisonCard()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lifetime Badge
            LifetimeBadge()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Single Purchase Button
            Button(
                onClick = onPurchase,
                enabled = !isPurchasing && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isPurchasing || isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "unlock pro for $priceFormatted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // "one-time purchase" reassurance
            Text(
                text = "one-time purchase • no subscription • yours forever",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // restore purchases
            TextButton(
                onClick = onRestore,
                enabled = !isPurchasing
            ) {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("restore previous purchase")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LifetimeBadge() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "lifetime license",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "pay once, own forever. no recurring charges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PaywallHero() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradient icon background
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "power tools for power users",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "4× more downloads • 4× faster speeds • torrents & more",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureComparisonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "free vs pro",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // core limits - these are the hook
            FeatureRow(
                feature = "simultaneous downloads",
                free = "4",
                pro = "12",
                icon = Icons.Outlined.Layers,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "connections per file",
                free = "7",
                pro = "16",
                icon = Icons.Outlined.Cable,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "download queue",
                free = "10",
                pro = "∞",
                icon = Icons.Outlined.Queue,
                proHighlight = true
            )
            
            // power features (hard gate)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "power features",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            FeatureRow(
                feature = "bittorrent / magnet",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.Link,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "smart scheduler",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.Schedule,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "video sniffer",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.VideoLibrary,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "batch url import",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.PlaylistAdd,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "auto-extract archives",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.FolderZip,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "download analytics",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.Analytics,
                proHighlight = true
            )
            
            FeatureRow(
                feature = "custom themes",
                free = "✗",
                pro = "✓",
                icon = Icons.Outlined.Palette,
                proHighlight = true
            )
        }
    }
}

@Composable
private fun FeatureRow(
    feature: String,
    free: String,
    pro: String,
    icon: ImageVector,
    proHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = free,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = pro,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (proHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (proHighlight) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Bottom sheet variant for quick paywall display (lifetime only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    priceFormatted: String,
    isPurchasing: Boolean,
    errorMessage: String?,
    triggerReason: PaywallTriggerReason,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trigger-specific message
            TriggerReasonBanner(triggerReason)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // simple lifetime purchase button
            Button(
                onClick = onPurchase,
                enabled = !isPurchasing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("unlock pro for $priceFormatted", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "one-time purchase • yours forever",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Error Message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // restore link
            TextButton(onClick = onRestore, enabled = !isPurchasing) {
                Text("restore previous purchase")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

enum class PaywallTriggerReason {
    // Limit triggers (user hit a cap)
    CONCURRENT_LIMIT,
    QUEUE_LIMIT,
    
    // Power feature triggers (hard gates)
    TORRENT_BLOCKED,
    SCHEDULER_BLOCKED,
    VIDEO_SNIFFER_BLOCKED,
    BATCH_IMPORT_BLOCKED,
    SPEED_BOOST_BLOCKED,
    CUSTOM_THEMES_BLOCKED,
    AUTO_EXTRACT_BLOCKED,
    BROWSER_INTEGRATION_BLOCKED,
    
    // Generic upgrade prompt
    SETTINGS_UPGRADE
}

@Composable
private fun TriggerReasonBanner(reason: PaywallTriggerReason) {
    val (icon, title, subtitle) = when (reason) {
        PaywallTriggerReason.CONCURRENT_LIMIT -> Triple(
            Icons.Outlined.Download,
            "download limit reached",
            "upgrade to pro for 12 simultaneous downloads"
        )
        PaywallTriggerReason.QUEUE_LIMIT -> Triple(
            Icons.Outlined.Queue,
            "queue is full",
            "upgrade to pro for unlimited queue size"
        )
        PaywallTriggerReason.TORRENT_BLOCKED -> Triple(
            Icons.Outlined.Link,
            "bittorrent is a pro feature",
            "upgrade to download torrents and magnet links"
        )
        PaywallTriggerReason.SCHEDULER_BLOCKED -> Triple(
            Icons.Outlined.Schedule,
            "smart scheduler is pro only",
            "schedule downloads for off-peak hours"
        )
        PaywallTriggerReason.VIDEO_SNIFFER_BLOCKED -> Triple(
            Icons.Outlined.VideoLibrary,
            "video sniffer is pro only",
            "detect and download streaming videos"
        )
        PaywallTriggerReason.BATCH_IMPORT_BLOCKED -> Triple(
            Icons.Outlined.PlaylistAdd,
            "batch import is pro only",
            "import multiple urls at once from text or file"
        )
        PaywallTriggerReason.SPEED_BOOST_BLOCKED -> Triple(
            Icons.Outlined.Bolt,
            "speed boost is pro only",
            "16 connections per file for 4× faster downloads"
        )
        PaywallTriggerReason.CUSTOM_THEMES_BLOCKED -> Triple(
            Icons.Outlined.Palette,
            "custom themes are pro only",
            "personalize your app with custom colors"
        )
        PaywallTriggerReason.AUTO_EXTRACT_BLOCKED -> Triple(
            Icons.Outlined.FolderZip,
            "auto-extract is pro only",
            "automatically extract archives after download"
        )
        PaywallTriggerReason.BROWSER_INTEGRATION_BLOCKED -> Triple(
            Icons.Outlined.Public,
            "browser integration is pro only",
            "intercept downloads from your browser"
        )
        PaywallTriggerReason.SETTINGS_UPGRADE -> Triple(
            Icons.Outlined.Bolt,
            "unlock full power",
            "get all pro features with one upgrade"
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
