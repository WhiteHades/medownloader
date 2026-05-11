package com.medownloader.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.medownloader.ui.theme.MonoTextStyleLarge
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
    var range by rememberSaveable { mutableStateOf(StatsRange.SEVEN_DAYS) }
    val liveActiveCount = remember(downloads) {
        downloads.count {
            it.status == DownloadStatus.ACTIVE ||
                it.status == DownloadStatus.PAUSED ||
                it.status == DownloadStatus.QUEUED
        }
    }
    val snapshot = remember(history, liveActiveCount, range) {
        buildStatsSnapshot(history, liveActiveCount, range)
    }

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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { HeroCard(snapshot = snapshot) }

            item {
                TimeRangeSelector(
                    selected = range,
                    onSelect = { range = it }
                )
            }

            item { MetricGrid(snapshot = snapshot) }

            item { StatusBreakdownCard(snapshot = snapshot) }

            item { DailyVolumeCard(snapshot = snapshot) }

            item { ActivityHeatmapCard(snapshot = snapshot) }

            item { FileTypesCard(snapshot = snapshot) }

            item {
                QuietSectionHeader(
                    title = stringResource(R.string.stats_history_title),
                    subtitle = stringResource(R.string.stats_history_subtitle)
                )
            }

            if (history.isEmpty()) {
                item { EmptyHistoryRow() }
            } else {
                items(history.take(15), key = { it.id }) { entry ->
                    HistoryRow(entry = entry)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun HeroCard(snapshot: StatsSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardHero
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_hero_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TrendChip(delta = snapshot.trendBytesDelta, isAllTime = snapshot.range.isAllTime)
            }

            Text(
                text = formatSize(snapshot.totalBytesDownloaded),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = if (snapshot.completedCount == 0) {
                    stringResource(R.string.stats_hero_count_zero)
                } else {
                    stringResource(R.string.stats_hero_count_fmt, snapshot.completedCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!snapshot.range.isAllTime && snapshot.allTimeBytes > snapshot.totalBytesDownloaded) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatSize(snapshot.allTimeBytes)} · ${snapshot.allTimeCompletedCount} all-time",
                        style = MonoTextStyleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendChip(delta: Long, isAllTime: Boolean) {
    if (isAllTime || delta == 0L) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = ExpressiveShapeTokens.Full
        ) {
            Text(
                text = stringResource(R.string.stats_trend_flat),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val positive = delta > 0
    val bg = if (positive) SuccessGreen90 else MaterialTheme.colorScheme.errorContainer
    val fg = if (positive) SuccessGreen40 else MaterialTheme.colorScheme.error
    val icon = if (positive) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown
    val label = if (positive) {
        stringResource(R.string.stats_trend_up_fmt, formatSize(kotlin.math.abs(delta)))
    } else {
        stringResource(R.string.stats_trend_down_fmt, formatSize(kotlin.math.abs(delta)))
    }

    Surface(color = bg, shape = ExpressiveShapeTokens.Full) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = stringResource(R.string.stats_trend_content_desc),
                modifier = Modifier.size(14.dp),
                tint = fg
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selected: StatsRange,
    onSelect: (StatsRange) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ExpressiveShapeTokens.Full
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsRange.values().forEach { option ->
                RangePill(
                    option = option,
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RangePill(
    option: StatsRange,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (option) {
        StatsRange.SEVEN_DAYS -> stringResource(R.string.stats_range_7d)
        StatsRange.THIRTY_DAYS -> stringResource(R.string.stats_range_30d)
        StatsRange.ALL_TIME -> stringResource(R.string.stats_range_all)
    }
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        color = bg,
        shape = ExpressiveShapeTokens.Full,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MetricGrid(snapshot: StatsSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = stringResource(R.string.stats_metric_completed),
                value = snapshot.completedCount.toString(),
                icon = Icons.Outlined.CheckCircle,
                accent = SuccessGreen40,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = stringResource(R.string.stats_metric_failed),
                value = snapshot.failedCount.toString(),
                icon = Icons.Outlined.Cancel,
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = stringResource(R.string.stats_metric_avg_speed),
                value = formatSpeed(snapshot.averageSpeed),
                icon = Icons.Outlined.Speed,
                accent = MaterialTheme.colorScheme.primary,
                mono = true,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = stringResource(R.string.stats_metric_peak_speed),
                value = formatSpeed(snapshot.peakSpeed),
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                accent = MaterialTheme.colorScheme.tertiary,
                mono = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    mono: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = ExpressiveShapes.small
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp),
                    tint = accent
                )
            }
            Text(
                text = value,
                style = if (mono) MonoTextStyleLarge else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBreakdownCard(snapshot: StatsSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardBold
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CardHeader(
                title = stringResource(R.string.stats_status_title),
                icon = Icons.Outlined.Timeline
            )

            if (snapshot.totalForStatusBar == 0) {
                Text(
                    text = stringResource(R.string.stats_status_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                StackedStatusBar(
                    completed = snapshot.completedRatio,
                    failed = snapshot.failedRatio,
                    active = snapshot.activeRatio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusLegend(
                        color = SuccessGreen40,
                        label = stringResource(R.string.stats_status_completed),
                        value = snapshot.completedCount
                    )
                    StatusLegend(
                        color = MaterialTheme.colorScheme.error,
                        label = stringResource(R.string.stats_status_failed),
                        value = snapshot.failedCount
                    )
                    StatusLegend(
                        color = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.stats_status_active),
                        value = snapshot.activeCount
                    )
                }
            }
        }
    }
}

@Composable
private fun StackedStatusBar(
    completed: Float,
    failed: Float,
    active: Float,
    modifier: Modifier
) {
    val animatedCompleted by animateFloatAsState(
        targetValue = completed,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "completedBar"
    )
    val animatedFailed by animateFloatAsState(
        targetValue = failed,
        animationSpec = tween(700, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "failedBar"
    )
    val animatedActive by animateFloatAsState(
        targetValue = active,
        animationSpec = tween(700, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "activeBar"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val completedColor = SuccessGreen40
    val failedColor = MaterialTheme.colorScheme.error
    val activeColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.clip(RoundedCornerShape(percent = 50))) {
        drawRect(color = trackColor)
        var x = 0f
        val segments = listOf(
            animatedCompleted to completedColor,
            animatedFailed to failedColor,
            animatedActive to activeColor
        )
        segments.forEach { (ratio, color) ->
            if (ratio <= 0f) return@forEach
            val width = size.width * ratio
            drawRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(width, size.height)
            )
            x += width
        }
    }
}

@Composable
private fun StatusLegend(color: Color, label: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(8.dp), color = color, shape = ExpressiveShapeTokens.Full) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
private fun DailyVolumeCard(snapshot: StatsSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardBold
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardHeader(
                    title = stringResource(R.string.stats_volume_title),
                    icon = Icons.Outlined.CloudDownload
                )
            }

            if (snapshot.dailyVolume.all { it.bytes == 0L }) {
                Text(
                    text = stringResource(R.string.stats_volume_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.stats_volume_subtitle_fmt,
                        formatSize(snapshot.peakDailyVolume)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DailyVolumeBars(
                    snapshot = snapshot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    snapshot.dailyVolume.filterIndexed { index, _ ->
                        index == 0 ||
                            index == snapshot.dailyVolume.lastIndex ||
                            index == snapshot.dailyVolume.size / 2
                    }.forEach { point ->
                        Text(
                            text = point.label,
                            style = MonoTextStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyVolumeBars(
    snapshot: StatsSnapshot,
    modifier: Modifier
) {
    val max = snapshot.peakDailyVolume.coerceAtLeast(1L)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val count = snapshot.dailyVolume.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = slotWidth * 0.6f
        snapshot.dailyVolume.forEachIndexed { index, point ->
            val ratio = point.bytes.toFloat() / max
            val barHeight = (size.height * ratio).coerceAtLeast(4f)
            val left = index * slotWidth + (slotWidth - barWidth) / 2f

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun ActivityHeatmapCard(snapshot: StatsSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CardHeader(
                title = stringResource(R.string.stats_activity_title),
                icon = Icons.Outlined.Timeline
            )
            Text(
                text = stringResource(R.string.stats_activity_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Heatmap(
                cells = snapshot.heatmap,
                accent = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            HeatmapLegend(accent = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Heatmap(
    cells: List<HeatmapCell>,
    accent: Color,
    trackColor: Color,
    modifier: Modifier
) {
    if (cells.isEmpty()) return
    val weeks = 12
    val days = 7
    val maxBytes = (cells.maxOfOrNull { it.bytes } ?: 1L).coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        val gap = 3.dp.toPx()
        val cellW = (size.width - gap * (weeks - 1)) / weeks
        val cellH = (size.height - gap * (days - 1)) / days
        val radius = cellW.coerceAtMost(cellH) / 4f

        cells.forEachIndexed { index, cell ->
            val week = index / days
            val day = index % days
            val x = week * (cellW + gap)
            val y = day * (cellH + gap)
            val intensity = if (cell.bytes <= 0L) {
                0f
            } else {
                0.25f + 0.75f * (cell.bytes.toFloat() / maxBytes)
            }
            val color = if (intensity == 0f) trackColor else accent.copy(alpha = intensity.coerceIn(0f, 1f))
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cellW, cellH),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

@Composable
private fun HeatmapLegend(accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_activity_legend_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(0.15f, 0.35f, 0.55f, 0.75f, 1f).forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (alpha == 0.15f) MaterialTheme.colorScheme.surfaceContainerHighest
                            else accent.copy(alpha = alpha)
                        )
                )
            }
        }
        Text(
            text = stringResource(R.string.stats_activity_legend_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileTypesCard(snapshot: StatsSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CardHeader(
                title = stringResource(R.string.stats_file_types_title),
                icon = Icons.Outlined.Storage
            )

            if (snapshot.fileTypes.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_file_types_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.stats_file_types_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val totalCount = snapshot.fileTypes.sumOf { it.count }.coerceAtLeast(1)
            snapshot.fileTypes.take(5).forEach { stat ->
                FileTypeRow(stat = stat, totalCount = totalCount)
            }
        }
    }
}

@Composable
private fun FileTypeRow(stat: FileTypeStat, totalCount: Int) {
    val ratio = (stat.count.toFloat() / totalCount).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "fileTypeRatio"
    )
    val accent = fileTypeAccent(stat.type)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    fileTypeIcon(stat.type),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accent
                )
                Text(
                    text = stat.type.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatSize(stat.bytes),
                    style = MonoTextStyleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.stats_file_types_count_fmt, stat.count),
                    style = MonoTextStyleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent)
            )
        }
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuietSectionHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
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
private fun HistoryRow(entry: DownloadHistoryEntry) {
    val isComplete = entry.status == DownloadStatus.COMPLETE.name
    val accent = if (isComplete) SuccessGreen40 else MaterialTheme.colorScheme.error
    val icon = if (isComplete) Icons.Filled.CheckCircle else Icons.Filled.Error

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ExpressiveShapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accent
                )
                Text(
                    text = entry.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatSize(entry.totalBytes),
                    style = MonoTextStyleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(formatTimestamp(entry.finishedAtEpochMs))
                    if (entry.durationMs > 0) {
                        append(" · ")
                        append(formatDuration(entry.durationMs))
                    }
                    if (entry.averageSpeed > 0) {
                        append(" · ")
                        append(formatSpeed(entry.averageSpeed))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyHistoryRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.stats_history_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.stats_history_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun fileTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "zip", "rar", "7z", "tar", "gz", "xz" -> Icons.Outlined.FolderZip
    "mp4", "mkv", "avi", "webm", "mov" -> Icons.Outlined.VideoFile
    "mp3", "flac", "wav", "aac", "ogg", "opus" -> Icons.Outlined.AudioFile
    "pdf" -> Icons.Outlined.PictureAsPdf
    "apk", "exe", "msi", "deb", "rpm" -> Icons.Outlined.InstallMobile
    "iso", "img", "dmg" -> Icons.Outlined.Album
    else -> Icons.Outlined.Storage
}

@Composable
private fun fileTypeAccent(type: String): Color = when (type.lowercase()) {
    "zip", "rar", "7z", "tar", "gz", "xz" -> MaterialTheme.colorScheme.tertiary
    "mp4", "mkv", "avi", "webm", "mov" -> MaterialTheme.colorScheme.secondary
    "mp3", "flac", "wav", "aac", "ogg", "opus" -> MaterialTheme.colorScheme.secondary
    "pdf" -> ErrorRed40
    "apk", "exe", "msi", "deb", "rpm" -> SuccessGreen40
    "iso", "img", "dmg" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.outline
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return formatEta(totalSeconds)
}

private fun formatTimestamp(epochMs: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMs))
}
