package com.medownloader.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medownloader.R
import com.medownloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    isPremium: Boolean,
    proPrice: String,
    wifiOnly: Boolean,
    maxConcurrent: Int,
    connectionLimit: Int,
    splitCount: Int,
    enableDht: Boolean,
    dnsServers: String,
    diskCacheMb: Int,
    downloadPath: String,
    onThemeSelected: (AppTheme) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onMaxConcurrentChanged: (Int) -> Unit,
    onConnectionLimitChanged: (Int) -> Unit,
    onSplitCountChanged: (Int) -> Unit,
    onEnableDhtChanged: (Boolean) -> Unit,
    onDnsServersChanged: (String) -> Unit,
    onDiskCacheMbChanged: (Int) -> Unit,
    onDownloadPathClick: () -> Unit,
    onBackClick: () -> Unit,
    onGetProClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showConnectionsDialog by remember { mutableStateOf(false) }
    var showConcurrentDialog by remember { mutableStateOf(false) }
    var showSplitCountDialog by remember { mutableStateOf(false) }
    var showDnsServersDialog by remember { mutableStateOf(false) }
    var showDiskCacheDialog by remember { mutableStateOf(false) }

    val connectionCountText = stringResource(
        R.string.settings_connection_count_fmt,
        connectionLimit,
        if (connectionLimit > 1) "s" else ""
    )
    val connectionSubtitle = if (isPremium) {
        "$connectionCountText • ${stringResource(R.string.settings_connections_subtitle)}"
    } else {
        "$connectionCountText ${stringResource(R.string.settings_connection_free_limit)}"
    }
    val concurrentCountText = stringResource(
        R.string.settings_concurrent_count_fmt,
        maxConcurrent,
        if (maxConcurrent > 1) "s" else ""
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back_content_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Premium Card (if not premium)
            if (!isPremium) {
                item {
                    PremiumUpgradeCard(
                        proPrice = proPrice,
                        onUpgradeClick = onGetProClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Appearance Section
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_appearance),
                    icon = Icons.Outlined.Palette
                )
            }
            
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.ColorLens,
                        title = stringResource(R.string.settings_theme),
                        subtitle = currentTheme.displayName(),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showThemeDialog = true
                        },
                        showChevron = true,
                        premiumBadge = !isPremium && currentTheme != AppTheme.DEFAULT
                    )
                }
            }
            
            // Downloads Section
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_downloads),
                    icon = Icons.Outlined.CloudDownload
                )
            }
            
            item {
                SettingsCard {
                    // WiFi Only toggle
                    SettingsToggleItem(
                        icon = Icons.Outlined.Wifi,
                        title = stringResource(R.string.settings_wifi_only),
                        subtitle = if (wifiOnly) stringResource(R.string.settings_wifi_only_desc_on) else stringResource(R.string.settings_wifi_only_desc_off),
                        checked = wifiOnly,
                        onCheckedChange = onWifiOnlyChanged
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.Speed,
                        title = stringResource(R.string.settings_connection_limit),
                        subtitle = connectionSubtitle,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showConnectionsDialog = true
                        },
                        showChevron = true,
                        premiumBadge = false
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.Queue,
                        title = stringResource(R.string.settings_max_concurrent),
                        subtitle = concurrentCountText,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showConcurrentDialog = true
                        },
                        showChevron = true,
                        premiumBadge = false
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.Folder,
                        title = stringResource(R.string.settings_download_location),
                        subtitle = downloadPath,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onDownloadPathClick()
                        },
                        showChevron = true
                    )
                }
            }
            
            // Advanced Downloads Section
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_advanced_downloads),
                    icon = Icons.Outlined.Tune
                )
            }
            
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.GridOn,
                        title = stringResource(R.string.settings_split_count),
                        subtitle = stringResource(R.string.settings_split_count_subtitle_fmt, splitCount),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showSplitCountDialog = true
                        },
                        showChevron = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsItem(
                        icon = Icons.Outlined.Dns,
                        title = stringResource(R.string.settings_dns_servers),
                        subtitle = dnsServers,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showDnsServersDialog = true
                        },
                        showChevron = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsItem(
                        icon = Icons.Outlined.Memory,
                        title = stringResource(R.string.settings_disk_cache),
                        subtitle = stringResource(R.string.settings_disk_cache_value_fmt, diskCacheMb),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showDiskCacheDialog = true
                        },
                        showChevron = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsItem(
                        icon = if (enableDht) Icons.Outlined.Share else Icons.Outlined.Share,
                        title = "DHT peer discovery",
                        subtitle = if (enableDht) "enabled" else "disabled",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onEnableDhtChanged(!enableDht)
                        },
                        showChevron = false
                    )
                }
            }
            
            // About Section
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_about),
                    icon = Icons.Outlined.Info
                )
            }
            
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.settings_version),
                        subtitle = stringResource(R.string.settings_version_value),
                        onClick = { },
                        showChevron = false
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = stringResource(R.string.settings_made_with_love),
                        subtitle = stringResource(R.string.settings_made_with_love_value),
                        onClick = { },
                        showChevron = false
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.BugReport,
                        title = stringResource(R.string.settings_feature_requests),
                        subtitle = stringResource(R.string.settings_feature_requests_value),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            uriHandler.openUri("https://github.com/WhiteHades/medownloader/issues/new")
                        },
                        showChevron = true
                    )
                }
            }
            
            // Premium status
            if (isPremium) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumStatusCard()
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            isPremium = isPremium,
            onThemeSelected = { theme ->
                onThemeSelected(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
            onUpgradeClick = {
                showThemeDialog = false
                onGetProClick()
            }
        )
    }
    
    // Connection Limit Dialog
    if (showConnectionsDialog) {
        ConnectionsDialog(
            title = stringResource(R.string.settings_connection_limit),
            subtitle = stringResource(R.string.settings_dialog_max_connections_subtitle),
            currentValue = connectionLimit,
            isPremium = isPremium,
            freeLimit = 7,
            onValueSelected = { value ->
                onConnectionLimitChanged(value)
                showConnectionsDialog = false
            },
            onUpgradeClick = {
                showConnectionsDialog = false
                onGetProClick()
            },
            onDismiss = { showConnectionsDialog = false }
        )
    }

    if (showConcurrentDialog) {
        ConnectionsDialog(
            title = stringResource(R.string.settings_max_concurrent),
            subtitle = stringResource(R.string.settings_concurrent_subtitle),
            currentValue = maxConcurrent,
            isPremium = isPremium,
            freeLimit = 4,
            options = listOf(1, 2, 3, 4, 6, 8, 12),
            onValueSelected = { value ->
                onMaxConcurrentChanged(value)
                showConcurrentDialog = false
            },
            onUpgradeClick = {
                showConcurrentDialog = false
                onGetProClick()
            },
            onDismiss = { showConcurrentDialog = false }
        )
    }
    
    // Split Count Dialog
    if (showSplitCountDialog) {
        ConnectionsDialog(
            title = stringResource(R.string.settings_split_count),
            subtitle = stringResource(R.string.settings_split_count_dialog_subtitle),
            currentValue = splitCount,
            isPremium = true,
            freeLimit = 32,
            options = listOf(1, 2, 4, 8, 16, 32),
            onValueSelected = { value ->
                onSplitCountChanged(value)
                showSplitCountDialog = false
            },
            onUpgradeClick = { showSplitCountDialog = false },
            onDismiss = { showSplitCountDialog = false }
        )
    }

    if (showDnsServersDialog) {
        TextValueDialog(
            title = stringResource(R.string.settings_dns_servers),
            subtitle = stringResource(R.string.settings_dns_servers_dialog_subtitle),
            initialValue = dnsServers,
            placeholder = stringResource(R.string.settings_dns_servers_placeholder),
            onValueSelected = { value ->
                onDnsServersChanged(value)
                showDnsServersDialog = false
            },
            onDismiss = { showDnsServersDialog = false }
        )
    }

    // Disk Cache Dialog
    if (showDiskCacheDialog) {
        ConnectionsDialog(
            title = stringResource(R.string.settings_disk_cache),
            subtitle = stringResource(R.string.settings_disk_cache_dialog_subtitle),
            currentValue = diskCacheMb,
            isPremium = true,
            freeLimit = 128,
            options = listOf(4, 8, 16, 32, 64, 128),
            onValueSelected = { value ->
                onDiskCacheMbChanged(value)
                showDiskCacheDialog = false
            },
            onUpgradeClick = { showDiskCacheDialog = false },
            onDismiss = { showDiskCacheDialog = false }
        )
    }
}

@Composable
private fun TextValueDialog(
    title: String,
    subtitle: String,
    initialValue: String,
    placeholder: String,
    onValueSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val doneLabel = stringResource(R.string.common_done)
    val cancelLabel = stringResource(android.R.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(placeholder) },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValueSelected(value.trim()) }) {
                Text(doneLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
        shape = ExpressiveShapeTokens.Dialog
    )
}

@Composable
private fun PremiumUpgradeCard(
    proPrice: String,
    onUpgradeClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = ExpressiveMotion.MicroBounce,
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onUpgradeClick
            ),
        shape = ExpressiveShapeTokens.CardHero,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // M3E wavy indicator with slow animation
            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "slowMorph")
                val slowProgress by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "progress"
                )
                
                CircularWavyProgressIndicator(
                    progress = { slowProgress },
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_pro_upgrade),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.settings_pro_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = ExpressiveShapeTokens.Full
            ) {
                Text(
                    text = proPrice,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PremiumStatusCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column {
                Text(
                    text = stringResource(R.string.settings_pro_user),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.settings_pro_thanks),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean,
    premiumBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (premiumBadge) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = ExpressiveShapeTokens.Full
                    ) {
                        Text(
                            text = stringResource(R.string.settings_pro_badge),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        if (showChevron) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    isPremium: Boolean,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val doneLabel = stringResource(R.string.common_done)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
                Text(
                    text = stringResource(R.string.settings_choose_theme),
                    fontWeight = FontWeight.Bold
                )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTheme.entries.forEach { theme ->
                    val isLocked = !isPremium && theme != AppTheme.DEFAULT
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveShapes.medium)
                            .selectable(
                                selected = currentTheme == theme,
                                onClick = {
                                    if (isLocked) {
                                        onUpgradeClick()
                                    } else {
                                        onThemeSelected(theme)
                                    }
                                }
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = null,
                            enabled = !isLocked
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = theme.displayName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentTheme == theme) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isLocked)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isLocked) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = stringResource(R.string.settings_pro_badge),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        
                        // Theme preview colors
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                modifier = Modifier.size(16.dp),
                                color = theme.previewColors.first,
                                shape = ExpressiveShapeTokens.Full
                            ) {}
                            Surface(
                                modifier = Modifier.size(16.dp),
                                color = theme.previewColors.second,
                                shape = ExpressiveShapeTokens.Full
                            ) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(doneLabel)
            }
        },
        shape = ExpressiveShapeTokens.Dialog
    )
}

@Composable
private fun ConnectionsDialog(
    title: String,
    subtitle: String,
    currentValue: Int,
    isPremium: Boolean = true,
    freeLimit: Int = 7,
    options: List<Int> = listOf(1, 2, 4, 8, 16),
    onValueSelected: (Int) -> Unit,
    onUpgradeClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val doneLabel = stringResource(R.string.common_done)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                options.forEach { value ->
                    val isLocked = !isPremium && value > freeLimit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveShapes.medium)
                            .selectable(
                                selected = currentValue == value,
                                onClick = {
                                    if (isLocked) {
                                        onUpgradeClick()
                                    } else {
                                        onValueSelected(value)
                                    }
                                }
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == value,
                            onClick = null,
                            enabled = !isLocked
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_connection_value_fmt,
                                value,
                                if (value > 1) "s" else ""
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentValue == value) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = ExpressiveShapeTokens.Full
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_pro_badge),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(doneLabel)
            }
        },
        shape = ExpressiveShapeTokens.Dialog
    )
}

// Theme helpers
@Composable
private fun AppTheme.displayName(): String = when (this) {
    AppTheme.DEFAULT -> stringResource(R.string.theme_default)
    AppTheme.CATPPUCCIN -> stringResource(R.string.theme_catppuccin_mocha)
    AppTheme.TOKYO_NIGHT -> stringResource(R.string.theme_tokyo_night)
    AppTheme.GRUVBOX -> stringResource(R.string.theme_gruvbox_dark)
    AppTheme.NORD -> stringResource(R.string.theme_nord)
}

val AppTheme.previewColors: Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>
    get() = when (this) {
        AppTheme.DEFAULT -> ElectricBlue40 to CyberViolet40
        AppTheme.CATPPUCCIN -> CatppuccinMocha.mauve to CatppuccinMocha.pink
        AppTheme.TOKYO_NIGHT -> TokyoNight.blue to TokyoNight.magenta
        AppTheme.GRUVBOX -> GruvboxDark.aqua to GruvboxDark.yellow
        AppTheme.NORD -> Nord.frost1 to Nord.frost2
    }
