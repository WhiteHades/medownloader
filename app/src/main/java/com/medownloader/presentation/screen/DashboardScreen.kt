package com.medownloader.presentation.screen
import com.medownloader.util.formatEta
import com.medownloader.util.formatSize
import com.medownloader.util.formatSpeed
import androidx.compose.ui.res.stringResource

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medownloader.R
import com.medownloader.data.model.Download
import com.medownloader.data.model.DownloadStatus
import com.medownloader.presentation.MainUiState
import com.medownloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    uiState: MainUiState,
    isPremium: Boolean,
    onAddClick: () -> Unit,
    onPauseClick: (String) -> Unit,
    onResumeClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    Scaffold(
        topBar = {
            ExpressiveTopBar(
                totalSpeed = uiState.globalStats?.totalDownloadSpeed ?: 0L,
                isPremium = isPremium,
                onSettingsClick = onSettingsClick,
                onStatsClick = onStatsClick
            )
        },
        floatingActionButton = {
            ExpressiveFab(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onAddClick()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.downloads.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Stats Bar
                item {
                    QuickStatsRow(
                        activeCount = uiState.activeDownloads.size,
                        completedCount = uiState.completedDownloads.size,
                        totalSpeed = uiState.globalStats?.totalDownloadSpeed ?: 0L
                    )
                }
                
                if (uiState.activeDownloads.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.dashboard_active),
                            count = uiState.activeDownloads.size,
                            icon = Icons.Outlined.CloudDownload
                        )
                    }
                    itemsIndexed(
                        uiState.activeDownloads,
                        key = { _, d -> d.gid }
                    ) { index, download ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200, delayMillis = index * 50)) +
                                    slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 500f
                                        ),
                                        initialOffsetY = { it / 2 }
                                    )
                        ) {
                            ExpressiveDownloadCard(
                                download = download,
                                isActive = true,
                                onPauseClick = { onPauseClick(download.gid) },
                                onResumeClick = { onResumeClick(download.gid) },
                                onRemoveClick = { onRemoveClick(download.gid) }
                            )
                        }
                    }
                }
                
                if (uiState.completedDownloads.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = stringResource(R.string.dashboard_completed),
                            count = uiState.completedDownloads.size,
                            icon = Icons.Outlined.CheckCircle
                        )
                    }
                    itemsIndexed(
                        uiState.completedDownloads,
                        key = { _, d -> d.gid }
                    ) { index, download ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(150, delayMillis = index * 30))
                        ) {
                            ExpressiveDownloadCard(
                                download = download,
                                isActive = false,
                                onPauseClick = { },
                                onResumeClick = { },
                                onRemoveClick = { onRemoveClick(download.gid) }
                            )
                        }
                    }
                }
                
                // Bottom spacing for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveTopBar(
    totalSpeed: Long,
    isPremium: Boolean,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (totalSpeed > 0) {
                    SpeedChip(speed = totalSpeed)
                }
            }
        },
        actions = {
            if (isPremium) {
                ProBadge()
            }
            IconButton(onClick = onStatsClick) {
                Icon(
                    Icons.Outlined.Analytics,
                    contentDescription = stringResource(R.string.dashboard_stats_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.dashboard_settings_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SpeedChip(speed: Long) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = ExpressiveShapeTokens.Full,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatSpeed(speed),
                style = MonoTextStyleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProBadge() {
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

@Composable
private fun ExpressiveFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Physics-based bounce animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = ExpressiveMotion.Bouncy,
        label = "fabScale"
    )
    
    val floatOffset = rememberFloatingAnimation()
    
    LargeFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .offset(y = (-floatOffset).dp),
        interactionSource = interactionSource,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = ExpressiveShapeTokens.Full
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = stringResource(R.string.dashboard_add_content_desc),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun QuickStatsRow(
    activeCount: Int,
    completedCount: Int,
    totalSpeed: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatMiniCard(
            icon = Icons.Outlined.CloudDownload,
            value = activeCount.toString(),
            label = stringResource(R.string.dashboard_active),
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        StatMiniCard(
            icon = Icons.Outlined.CheckCircle,
            value = completedCount.toString(),
            label = stringResource(R.string.dashboard_done),
            modifier = Modifier.weight(1f),
            containerColor = SuccessGreen90,
            contentColor = SuccessGreen30
        )
        StatMiniCard(
            icon = Icons.Outlined.Speed,
            value = formatSpeed(totalSpeed),
            label = stringResource(R.string.dashboard_speed),
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            useMono = true
        )
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    useMono: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = ExpressiveShapeTokens.StatsCard,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Text(
                text = value,
                style = if (useMono) MonoTextStyle else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ExpressiveDownloadCard(
    download: Download,
    isActive: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val view = LocalView.current
    
    // Pulse animation for active downloads
    val pulseScale = if (isActive && download.downloadSpeed > 0) {
        rememberPulseAnimation()
    } else 1f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale),
        shape = ExpressiveShapeTokens.CardBold,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File type icon
                FileTypeIcon(filename = download.filename, status = download.status)
                
                // Filename and status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.filename,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = getStatusText(download),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action buttons
                if (isActive) {
                    ActionButton(
                        icon = if (download.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            if (download.isPaused) onResumeClick() else onPauseClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Progress section for active downloads
            if (isActive && !download.isComplete) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Progress bar with expressive rounded caps
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(ExpressiveShapeTokens.ProgressTrack),
                        color = when (download.status) {
                            DownloadStatus.PAUSED -> MaterialTheme.colorScheme.outline
                            DownloadStatus.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Speed
                        if (download.downloadSpeed > 0) {
                            Text(
                                text = formatSpeed(download.downloadSpeed),
                                style = MonoTextStyleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = if (download.isPaused) stringResource(R.string.status_paused) else stringResource(R.string.status_starting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Progress %
                        Text(
                            text = "${download.progressPercent}%",
                            style = MonoTextStyleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Size
                        Text(
                            text = "${formatSize(download.completedLength)} / ${formatSize(download.totalLength)}",
                            style = MonoTextStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Completed state
            if (download.isComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SuccessGreen40
                        )
                        Text(
                            text = formatSize(download.totalLength),
                            style = MonoTextStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = { /* Open file */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = stringResource(R.string.dashboard_open_content_desc),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onRemoveClick,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.dashboard_remove_content_desc),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTypeIcon(filename: String, status: DownloadStatus) {
    val (icon, containerColor, contentColor) = when {
        status == DownloadStatus.ERROR -> Triple(
            Icons.Filled.ErrorOutline,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error
        )
        filename.endsWith(".zip") || filename.endsWith(".rar") || filename.endsWith(".7z") -> Triple(
            Icons.Outlined.FolderZip,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        filename.endsWith(".mp4") || filename.endsWith(".mkv") || filename.endsWith(".avi") -> Triple(
            Icons.Outlined.VideoFile,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        filename.endsWith(".mp3") || filename.endsWith(".flac") || filename.endsWith(".wav") -> Triple(
            Icons.Outlined.AudioFile,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        filename.endsWith(".pdf") -> Triple(
            Icons.Outlined.PictureAsPdf,
            ErrorRed90,
            ErrorRed40
        )
        filename.endsWith(".apk") || filename.endsWith(".exe") -> Triple(
            Icons.Outlined.InstallMobile,
            SuccessGreen90,
            SuccessGreen40
        )
        filename.endsWith(".iso") || filename.endsWith(".img") -> Triple(
            Icons.Outlined.Album,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> Triple(
            Icons.Outlined.InsertDriveFile,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    Surface(
        color = containerColor,
        shape = ExpressiveShapes.medium
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(10.dp),
            tint = contentColor
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = ExpressiveMotion.MicroBounce,
        label = "actionScale"
    )
    
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).scale(scale),
        interactionSource = interactionSource,
        // M3 Expressive: Use shapes parameter for morphing shape support
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Slow-animating progress for gentle morphing effect
        val infiniteTransition = rememberInfiniteTransition(label = "slowMorph")
        val slowProgress by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "progress"
        )
        
        val floatOffset = rememberFloatingAnimation(targetValue = 12f, durationMillis = 4000)
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(y = (-floatOffset).dp),
            contentAlignment = Alignment.Center
        ) {
            // M3E CircularWavyProgressIndicator with slow animation
            CircularWavyProgressIndicator(
                progress = { slowProgress },
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
            
            // Icon overlay
            Icon(
                Icons.Outlined.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.dashboard_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.dashboard_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getStatusText(download: Download): String {
    return when (download.status) {
        DownloadStatus.ACTIVE -> if (download.downloadSpeed > 0) {
            "ETA: ${formatEta(download.etaSeconds)}"
        } else stringResource(R.string.status_connecting)
        DownloadStatus.PAUSED -> stringResource(R.string.status_paused)
        DownloadStatus.WAITING -> stringResource(R.string.status_waiting)
        DownloadStatus.COMPLETE -> stringResource(R.string.status_completed)
        DownloadStatus.ERROR -> download.errorMessage ?: stringResource(R.string.status_error)
        DownloadStatus.REMOVED -> stringResource(R.string.status_removed)
    }
}

