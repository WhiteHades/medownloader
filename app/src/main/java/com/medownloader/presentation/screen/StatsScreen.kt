package com.medownloader.presentation.screen
import com.medownloader.util.formatSize
import com.medownloader.util.formatSpeed
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medownloader.data.model.Download
import com.medownloader.data.model.DownloadStatus
import com.medownloader.ui.theme.*

import com.medownloader.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(
    downloads: List<Download>,
    totalDownloaded: Long,
    averageSpeed: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = remember(downloads) { calculateStats(downloads) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.stats_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back_content_desc))
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.stats_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            item {
                OverviewCardsRow(stats = stats, totalDownloaded = totalDownloaded)
            }
            
            item {
                StatusDistributionCard(stats = stats)
            }
            
            item {
                Text(
                    text = stringResource(R.string.stats_speed_analysis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SpeedStatsCard(averageSpeed = averageSpeed, stats = stats)
            }
            
            item {
                Text(
                    text = stringResource(R.string.stats_file_types),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                FileTypesCard(fileTypeStats = stats.fileTypes)
            }
            
            if (downloads.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.stats_recent_performance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(downloads.take(5)) { download ->
                    DownloadPerformanceCard(download = download)
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OverviewCardsRow(stats: DownloadStats, totalDownloaded: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewStatCard(
            icon = Icons.Outlined.CloudDownload,
            value = stats.totalCount.toString(),
            label = stringResource(R.string.stats_total),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        OverviewStatCard(
            icon = Icons.Outlined.CheckCircle,
            value = stats.completedCount.toString(),
            label = stringResource(R.string.dashboard_completed),
            containerColor = SuccessGreen90,
            modifier = Modifier.weight(1f)
        )
        OverviewStatCard(
            icon = Icons.Outlined.Storage,
            value = formatSize(totalDownloaded),
            label = stringResource(R.string.stats_downloaded),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = ExpressiveShapeTokens.StatsCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatusDistributionCard(stats: DownloadStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardBold,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_status_distribution),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedRingChart(
                    completedPercent = if (stats.totalCount > 0) 
                        stats.completedCount.toFloat() / stats.totalCount else 0f,
                    activePercent = if (stats.totalCount > 0) 
                        stats.activeCount.toFloat() / stats.totalCount else 0f,
                    modifier = Modifier.size(120.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(
                        color = SuccessGreen40,
                        label = stringResource(R.string.dashboard_completed),
                        value = stats.completedCount
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.dashboard_active),
                        value = stats.activeCount
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.outline,
                        label = stringResource(R.string.status_paused),
                        value = stats.pausedCount
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.error,
                        label = stringResource(R.string.stats_failed),
                        value = stats.errorCount
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedRingChart(
    completedPercent: Float,
    activePercent: Float,
    modifier: Modifier = Modifier
) {
    val animatedCompleted by animateFloatAsState(
        targetValue = completedPercent,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "completed"
    )
    val animatedActive by animateFloatAsState(
        targetValue = activePercent,
        animationSpec = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "active"
    )
    
    val completedColor = SuccessGreen40
    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    
    Canvas(modifier = modifier) {
        val strokeWidth = 16.dp.toPx()
        
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        if (animatedCompleted > 0) {
            drawArc(
                color = completedColor,
                startAngle = -90f,
                sweepAngle = animatedCompleted * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        if (animatedActive > 0) {
            drawArc(
                color = activeColor,
                startAngle = -90f + animatedCompleted * 360f,
                sweepAngle = animatedActive * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = color,
            shape = ExpressiveShapeTokens.Full
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MonoTextStyleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SpeedStatsCard(averageSpeed: Long, stats: DownloadStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardSoft,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpeedStatItem(
                    label = stringResource(R.string.stats_average),
                    speed = averageSpeed,
                    icon = Icons.Outlined.Speed
                )
                SpeedStatItem(
                    label = stringResource(R.string.stats_peak),
                    speed = stats.peakSpeed,
                    icon = Icons.Outlined.TrendingUp
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Text(
                text = stringResource(R.string.stats_speed_history),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            SpeedBarChart(
                speeds = stats.recentSpeeds,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }
    }
}

@Composable
private fun SpeedStatItem(
    label: String,
    speed: Long,
    icon: ImageVector
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = ExpressiveShapes.small
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatSpeed(speed),
                style = MonoTextStyle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SpeedBarChart(speeds: List<Long>, modifier: Modifier = Modifier) {
    val maxSpeed = speeds.maxOrNull() ?: 1L
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    
    Canvas(modifier = modifier) {
        val barWidth = size.width / (speeds.size.coerceAtLeast(1) * 2f)
        val gap = barWidth
        
        speeds.forEachIndexed { index, speed ->
            val barHeight = if (maxSpeed > 0) (speed.toFloat() / maxSpeed) * size.height else 0f
            val x = index * (barWidth + gap)
            
            // Bar background
            drawRoundRect(
                color = containerColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2)
            )
            
            // Bar fill
            if (barHeight > 0) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
        }
    }
}

@Composable
private fun FileTypesCard(fileTypeStats: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardSoft,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (fileTypeStats.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                fileTypeStats.entries
                    .sortedByDescending { it.value }
                    .take(6)
                    .forEach { (type, count) ->
                        FileTypeRow(type = type, count = count, total = fileTypeStats.values.sum())
                    }
            }
        }
    }
}

@Composable
private fun FileTypeRow(type: String, count: Int, total: Int) {
    val percentage = count.toFloat() / total
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "fileType"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getFileTypeIcon(type),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = getFileTypeColor(type)
                )
                Text(
                    text = type.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = stringResource(R.string.stats_files_suffix, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LinearProgressIndicator(
            progress = { animatedPercentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(ExpressiveShapeTokens.Full),
            color = getFileTypeColor(type),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun DownloadPerformanceCard(download: Download) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Surface(
                color = when (download.status) {
                    DownloadStatus.COMPLETE -> SuccessGreen90
                    DownloadStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                    DownloadStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = ExpressiveShapes.small
            ) {
                Icon(
                    when (download.status) {
                        DownloadStatus.COMPLETE -> Icons.Filled.CheckCircle
                        DownloadStatus.ERROR -> Icons.Filled.Error
                        DownloadStatus.ACTIVE -> Icons.Filled.Download
                        else -> Icons.Outlined.Pause
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                    tint = when (download.status) {
                        DownloadStatus.COMPLETE -> SuccessGreen40
                        DownloadStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = formatSize(download.totalLength),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (download.downloadSpeed > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = ExpressiveShapeTokens.Full
                ) {
                    Text(
                        text = formatSpeed(download.downloadSpeed),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MonoTextStyleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getFileTypeIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.FolderZip
        "mp4", "mkv", "avi", "webm" -> Icons.Outlined.VideoFile
        "mp3", "flac", "wav", "aac" -> Icons.Outlined.AudioFile
        "pdf" -> Icons.Outlined.PictureAsPdf
        "apk", "exe" -> Icons.Outlined.InstallMobile
        "iso", "img" -> Icons.Outlined.Album
        else -> Icons.Outlined.InsertDriveFile
    }
}

@Composable
private fun getFileTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "zip", "rar", "7z", "tar", "gz" -> MaterialTheme.colorScheme.tertiary
        "mp4", "mkv", "avi", "webm" -> MaterialTheme.colorScheme.secondary
        "mp3", "flac", "wav", "aac" -> MaterialTheme.colorScheme.secondary
        "pdf" -> ErrorRed40
        "apk", "exe" -> SuccessGreen40
        "iso", "img" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
}

private fun calculateStats(downloads: List<Download>): DownloadStats {
    val fileTypes = downloads.groupBy { 
        it.filename.substringAfterLast('.', "unknown")
    }.mapValues { it.value.size }
    
    return DownloadStats(
        totalCount = downloads.size,
        completedCount = downloads.count { it.status == DownloadStatus.COMPLETE },
        activeCount = downloads.count { it.status == DownloadStatus.ACTIVE },
        pausedCount = downloads.count { it.status == DownloadStatus.PAUSED },
        errorCount = downloads.count { it.status == DownloadStatus.ERROR },
        fileTypes = fileTypes,
        peakSpeed = downloads.maxOfOrNull { it.downloadSpeed } ?: 0L,
        recentSpeeds = downloads.take(10).map { it.downloadSpeed }
    )
}

data class DownloadStats(
    val totalCount: Int,
    val completedCount: Int,
    val activeCount: Int,
    val pausedCount: Int,
    val errorCount: Int,
    val fileTypes: Map<String, Int>,
    val peakSpeed: Long,
    val recentSpeeds: List<Long>
)
