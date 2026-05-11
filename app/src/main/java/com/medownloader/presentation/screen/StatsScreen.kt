package com.medownloader.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medownloader.R
import com.medownloader.data.engine.DownloadProgress
import com.medownloader.data.engine.DownloadStatus
import com.medownloader.data.model.DownloadHistoryEntry
import com.medownloader.ui.theme.ErrorRed40
import com.medownloader.ui.theme.ExpressiveShapeTokens
import com.medownloader.ui.theme.ExpressiveShapes
import com.medownloader.ui.theme.MonoTextStyle
import com.medownloader.ui.theme.MonoTextStyleSmall
import com.medownloader.ui.theme.SuccessGreen40
import com.medownloader.ui.theme.SuccessGreen90
import com.medownloader.util.formatEta
import com.medownloader.util.formatSize
import com.medownloader.util.formatSpeed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    downloads: List<DownloadProgress>,
    history: List<DownloadHistoryEntry>,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = remember(downloads, history) { buildStatsSnapshot(downloads, history) }

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_content_desc)
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.stats_clear_history)
                            )
                        }
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
                SectionHeading(
                    title = stringResource(R.string.stats_overview),
                    subtitle = stringResource(R.string.stats_overview_subtitle)
                )
            }

            item {
                SummaryCards(snapshot = snapshot)
            }

            if (downloads.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = stringResource(R.string.stats_live_queue),
                        subtitle = stringResource(R.string.stats_live_queue_subtitle)
                    )
                }

                item {
                    LiveQueueCard(downloads = downloads)
                }
            }

            item {
                SectionHeading(
                    title = stringResource(R.string.stats_history_trend),
                    subtitle = stringResource(R.string.stats_history_trend_subtitle)
                )
            }

            item {
                DailyBytesCard(snapshot = snapshot)
            }

            item {
                StatusDistributionCard(snapshot = snapshot)
            }

            item {
                SpeedHistoryCard(snapshot = snapshot)
            }

            item {
                FileTypesCard(fileTypeStats = snapshot.fileTypes)
            }

            item {
                SectionHeading(
                    title = stringResource(R.string.stats_recent_history),
                    subtitle = stringResource(R.string.stats_recent_history_subtitle)
                )
            }

            if (history.isEmpty()) {
                item {
                    EmptyHistoryCard()
                }
            } else {
                items(history.take(12), key = { it.id }) { entry ->
                    HistoryEntryCard(entry = entry)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCards(snapshot: StatsSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewStatCard(
            icon = Icons.Outlined.CloudDownload,
            value = snapshot.completedCount.toString(),
            label = stringResource(R.string.stats_completed_total),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        OverviewStatCard(
            icon = Icons.Outlined.Storage,
            value = formatSize(snapshot.totalBytesDownloaded),
            label = stringResource(R.string.stats_downloaded),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
        OverviewStatCard(
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            value = formatSpeed(snapshot.averageHistoricalSpeed),
            label = stringResource(R.string.stats_average),
            containerColor = SuccessGreen90,
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
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveQueueCard(downloads: List<DownloadProgress>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardBold,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_live_queue),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.stats_live_count_fmt, downloads.size),
                    style = MonoTextStyleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            downloads.take(6).forEach { download ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = download.filename,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (download.status) {
                                DownloadStatus.ACTIVE -> formatSpeed(download.downloadSpeed)
                                DownloadStatus.PAUSED -> stringResource(R.string.status_paused)
                                DownloadStatus.QUEUED -> stringResource(R.string.status_waiting)
                                else -> download.status.name.lowercase()
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${download.progressPercent}%",
                        style = MonoTextStyleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun DailyBytesCard(snapshot: StatsSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardBold,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_history_trend),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            DailyBytesBarChart(
                dailyPoints = snapshot.dailyHistory,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
        }
    }
}

@Composable
private fun DailyBytesBarChart(dailyPoints: List<DailyHistoryPoint>, modifier: Modifier = Modifier) {
    val maxBytes = (dailyPoints.maxOfOrNull { it.bytes } ?: 1L).coerceAtLeast(1L)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val barBrush = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )
    Canvas(modifier = modifier) {
        val count = dailyPoints.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = slotWidth * 0.56f
        dailyPoints.forEachIndexed { index, point ->
            val barHeight = (point.bytes.toFloat() / maxBytes) * size.height
            val left = index * slotWidth + ((slotWidth - barWidth) / 2f)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun StatusDistributionCard(snapshot: StatsSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardBold,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                StatusRingChart(
                    completedPercent = snapshot.completedRatio,
                    failedPercent = snapshot.failedRatio,
                    activePercent = snapshot.activeRatio,
                    modifier = Modifier.size(126.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(SuccessGreen40, stringResource(R.string.dashboard_completed), snapshot.completedCount)
                    LegendItem(MaterialTheme.colorScheme.error, stringResource(R.string.stats_failed), snapshot.failedCount)
                    LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.dashboard_active), snapshot.activeCount)
                }
            }
        }
    }
}

@Composable
private fun StatusRingChart(
    completedPercent: Float,
    failedPercent: Float,
    activePercent: Float,
    modifier: Modifier = Modifier
) {
    val completed by animateFloatAsState(completedPercent, tween(900, easing = FastOutSlowInEasing), label = "completed")
    val failed by animateFloatAsState(failedPercent, tween(900, delayMillis = 120, easing = FastOutSlowInEasing), label = "failed")
    val active by animateFloatAsState(activePercent, tween(900, delayMillis = 240, easing = FastOutSlowInEasing), label = "active")
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val failedColor = MaterialTheme.colorScheme.error
    val activeColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val strokeWidth = 16.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        var start = -90f
        listOf(
            completed to SuccessGreen40,
            failed to failedColor,
            active to activeColor
        ).forEach { (value, color) ->
            if (value > 0f) {
                val sweep = value * 360f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                start += sweep
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), color = color, shape = ExpressiveShapeTokens.Full) {}
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value.toString(), style = MonoTextStyleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SpeedHistoryCard(snapshot: StatsSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardSoft,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpeedStatItem(stringResource(R.string.stats_average), snapshot.averageHistoricalSpeed, Icons.Outlined.Speed)
                SpeedStatItem(stringResource(R.string.stats_peak), snapshot.peakHistoricalSpeed, Icons.AutoMirrored.Outlined.TrendingUp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.stats_speed_history),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SpeedLineChart(
                speeds = snapshot.recentSpeeds,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }
    }
}

@Composable
private fun SpeedStatItem(label: String, speed: Long, icon: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = ExpressiveShapes.small) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = formatSpeed(speed), style = MonoTextStyle, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SpeedLineChart(speeds: List<Long>, modifier: Modifier = Modifier) {
    val points = if (speeds.isEmpty()) listOf(0L) else speeds
    val maxSpeed = (points.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val stepX = if (points.size == 1) size.width else size.width / (points.size - 1).toFloat()
        val path = Path()
        points.forEachIndexed { index, speed ->
            val x = index * stepX
            val y = size.height - ((speed.toFloat() / maxSpeed) * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, speed ->
            val x = index * stepX
            val y = size.height - ((speed.toFloat() / maxSpeed) * size.height)
            drawCircle(pointColor, radius = 5.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun FileTypesCard(fileTypeStats: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardSoft,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (fileTypeStats.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_empty_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                fileTypeStats.entries.sortedByDescending { it.value }.take(6).forEach { (type, count) ->
                    FileTypeRow(type = type, count = count, total = fileTypeStats.values.sum())
                }
            }
        }
    }
}

@Composable
private fun FileTypeRow(type: String, count: Int, total: Int) {
    val percentage = if (total == 0) 0f else count.toFloat() / total
    val animated by animateFloatAsState(percentage, tween(800, easing = FastOutSlowInEasing), label = "fileType")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(getFileTypeIcon(type), contentDescription = null, modifier = Modifier.size(16.dp), tint = getFileTypeColor(type))
                Text(text = type.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = stringResource(R.string.stats_files_suffix, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = getFileTypeColor(type),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.CardSoft,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.stats_empty_history),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.stats_empty_history_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: DownloadHistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (entry.status == DownloadStatus.COMPLETE.name) SuccessGreen90 else MaterialTheme.colorScheme.errorContainer,
                shape = ExpressiveShapes.small
            ) {
                Icon(
                    imageVector = if (entry.status == DownloadStatus.COMPLETE.name) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                    tint = if (entry.status == DownloadStatus.COMPLETE.name) SuccessGreen40 else MaterialTheme.colorScheme.error
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatSize(entry.totalBytes)} • ${formatDuration(entry.durationMs)} • ${formatTimestamp(entry.finishedAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (entry.averageSpeed > 0) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = ExpressiveShapeTokens.Full) {
                    Text(
                        text = formatSpeed(entry.averageSpeed),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MonoTextStyleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun getFileTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.FolderZip
    "mp4", "mkv", "avi", "webm" -> Icons.Outlined.VideoFile
    "mp3", "flac", "wav", "aac" -> Icons.Outlined.AudioFile
    "pdf" -> Icons.Outlined.PictureAsPdf
    "apk", "exe" -> Icons.Outlined.InstallMobile
    "iso", "img" -> Icons.Outlined.Album
    else -> Icons.Outlined.Storage
}

@Composable
private fun getFileTypeColor(type: String): Color = when (type.lowercase()) {
    "zip", "rar", "7z", "tar", "gz" -> MaterialTheme.colorScheme.tertiary
    "mp4", "mkv", "avi", "webm" -> MaterialTheme.colorScheme.secondary
    "mp3", "flac", "wav", "aac" -> MaterialTheme.colorScheme.secondary
    "pdf" -> ErrorRed40
    "apk", "exe" -> SuccessGreen40
    "iso", "img" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.outline
}

private fun buildStatsSnapshot(
    downloads: List<DownloadProgress>,
    history: List<DownloadHistoryEntry>
): StatsSnapshot {
    val completedHistory = history.filter { it.status == DownloadStatus.COMPLETE.name }
    val failedHistory = history.filter { it.status == DownloadStatus.ERROR.name }
    val fileTypes = history.groupBy { it.fileType.ifBlank { "unknown" } }.mapValues { it.value.size }
    val recentSpeeds = history.sortedByDescending { it.finishedAtEpochMs }.take(12).map { it.averageSpeed }
    val dailyHistory = history
        .groupBy { SimpleDateFormat("MM-dd", Locale.US).format(Date(it.finishedAtEpochMs)) }
        .entries
        .sortedBy { it.key }
        .takeLast(7)
        .map { (label, entries) -> DailyHistoryPoint(label, entries.sumOf { it.totalBytes }) }

    val totalCount = history.size + downloads.count { it.status == DownloadStatus.ACTIVE || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.QUEUED }
    val activeCount = downloads.count { it.status == DownloadStatus.ACTIVE || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.QUEUED }
    val completedCount = completedHistory.size
    val failedCount = failedHistory.size

    return StatsSnapshot(
        totalBytesDownloaded = completedHistory.sumOf { it.totalBytes },
        totalCount = totalCount,
        completedCount = completedCount,
        failedCount = failedCount,
        activeCount = activeCount,
        averageHistoricalSpeed = completedHistory.map { it.averageSpeed }.filter { it > 0 }.average().toLong(),
        peakHistoricalSpeed = history.maxOfOrNull { it.averageSpeed } ?: 0L,
        fileTypes = fileTypes,
        recentSpeeds = recentSpeeds.ifEmpty { listOf(0L) },
        dailyHistory = dailyHistory.ifEmpty { listOf(DailyHistoryPoint("today", 0L)) }
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return formatEta(totalSeconds)
}

private fun formatTimestamp(epochMs: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMs))
}

private data class StatsSnapshot(
    val totalBytesDownloaded: Long,
    val totalCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val activeCount: Int,
    val averageHistoricalSpeed: Long,
    val peakHistoricalSpeed: Long,
    val fileTypes: Map<String, Int>,
    val recentSpeeds: List<Long>,
    val dailyHistory: List<DailyHistoryPoint>
) {
    val completedRatio: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    val failedRatio: Float get() = if (totalCount == 0) 0f else failedCount.toFloat() / totalCount
    val activeRatio: Float get() = if (totalCount == 0) 0f else activeCount.toFloat() / totalCount
}

private data class DailyHistoryPoint(
    val label: String,
    val bytes: Long
)
