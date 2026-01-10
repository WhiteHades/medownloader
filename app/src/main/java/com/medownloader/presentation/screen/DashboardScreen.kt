package com.medownloader.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medownloader.data.model.Download
import com.medownloader.data.model.DownloadStatus
import com.medownloader.presentation.MainUiState
import com.medownloader.ui.theme.CardShape
import com.medownloader.ui.theme.ExpressiveMotion
import com.medownloader.ui.theme.MonoTextStyle
import com.medownloader.ui.theme.Shapes
import com.medownloader.ui.theme.rememberFloatingAnimation
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: MainUiState,
    isPremium: Boolean,
    onAddClick: () -> Unit,
    onPauseClick: (String) -> Unit,
    onResumeClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "meDownloader",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.globalStats != null) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "↓ ${formatSpeed(uiState.globalStats.totalDownloadSpeed)}",
                                style = MonoTextStyle,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                actions = {
                    if (isPremium) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("pro", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = ExpressiveMotion.QuickSnap,
                label = "fab"
            )

            ExtendedFloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onAddClick()
                },
                modifier = Modifier.scale(scale),
                interactionSource = interactionSource,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.Add, "add", modifier = Modifier.size(22.dp)) },
                text = { Text("new download", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        if (uiState.downloads.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    StatsBar(
                        activeCount = uiState.activeDownloads.size,
                        completedCount = uiState.completedDownloads.size,
                        totalSpeed = uiState.globalStats?.totalDownloadSpeed ?: 0L
                    )
                }

                if (uiState.activeDownloads.isNotEmpty()) {
                    item { SectionHeader("active") }
                    itemsIndexed(uiState.activeDownloads, key = { _, d -> d.gid }) { i, download ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(150, delayMillis = i * 30)) +
                                    slideInVertically(tween(150, delayMillis = i * 30)) { it / 3 }
                        ) {
                            DownloadCard(download, true, onPauseClick, onResumeClick, onRemoveClick)
                        }
                    }
                }

                if (uiState.completedDownloads.isNotEmpty()) {
                    item { SectionHeader("completed") }
                    itemsIndexed(uiState.completedDownloads, key = { _, d -> d.gid }) { i, download ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(150, delayMillis = i * 30))
                        ) {
                            DownloadCard(download, false, onPauseClick, onResumeClick, onRemoveClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsBar(activeCount: Int, completedCount: Int, totalSpeed: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(Icons.Outlined.CloudDownload, activeCount.toString(), "active", MaterialTheme.colorScheme.primary)
            StatItem(Icons.Default.Check, completedCount.toString(), "done", MaterialTheme.colorScheme.tertiary)
            StatItem(Icons.Outlined.Speed, formatSpeed(totalSpeed), "speed", MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MonoTextStyle, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
fun DownloadCard(
    download: Download,
    isActive: Boolean,
    onPauseClick: (String) -> Unit,
    onResumeClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(download.progress) {
        progress.animateTo(download.progress, spring(dampingRatio = 0.7f, stiffness = 400f))
    }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    StatusIcon(download.status)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(download.filename, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isActive) Text("${download.connections} connections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
            Box(Modifier.fillMaxWidth().height(5.dp).clip(Shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Box(Modifier.fillMaxWidth(progress.value).height(5.dp).clip(Shapes.extraSmall).background(
                    Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                ))
            }
            
            Spacer(Modifier.height(6.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (isActive) Text(formatSpeed(download.downloadSpeed), style = MonoTextStyle, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                else Text(if (download.isComplete) "complete" else "paused", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatBytes(download.completedLength)} / ${formatBytes(download.totalLength)}", style = MonoTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (!download.isComplete) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = { onRemoveClick(download.gid) }, contentPadding = PaddingValues(horizontal = 14.dp), shape = Shapes.small) { Text("cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (download.isActive) onPauseClick(download.gid) else onResumeClick(download.gid) },
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        shape = Shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = if (download.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(if (download.isActive) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(16.dp), tint = if (download.isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(4.dp))
                        Text(if (download.isActive) "pause" else "resume", color = if (download.isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIcon(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (bg, icon, tint) = when (status) {
        DownloadStatus.COMPLETE -> Triple(MaterialTheme.colorScheme.primaryContainer, Icons.Default.Check, MaterialTheme.colorScheme.onPrimaryContainer)
        DownloadStatus.ERROR -> Triple(MaterialTheme.colorScheme.errorContainer, Icons.Default.PriorityHigh, MaterialTheme.colorScheme.onErrorContainer)
        DownloadStatus.PAUSED -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, Icons.Default.Pause, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, Icons.Default.ArrowDownward, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(modifier.size(36.dp).clip(Shapes.small).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    val floatOffset = rememberFloatingAnimation()
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.offset { IntOffset(0, floatOffset.roundToInt()) }.size(100.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, Shapes.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CloudDownload, null, Modifier.size(50.dp), MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text("no downloads yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("tap + to add a download\nor share a link from your browser", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun formatSpeed(bytesPerSecond: Long) = when {
    bytesPerSecond >= 1_000_000 -> "%.1f mb/s".format(bytesPerSecond / 1_000_000.0)
    bytesPerSecond >= 1_000 -> "%.1f kb/s".format(bytesPerSecond / 1_000.0)
    else -> "$bytesPerSecond b/s"
}

private fun formatBytes(bytes: Long) = when {
    bytes >= 1_000_000_000 -> "%.2f gb".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f mb".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f kb".format(bytes / 1_000.0)
    else -> "$bytes b"
}
