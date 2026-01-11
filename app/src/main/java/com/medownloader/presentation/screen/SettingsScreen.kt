package com.medownloader.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medownloader.ui.theme.AppTheme
import com.medownloader.ui.theme.CardShape
import com.medownloader.ui.theme.ExpressiveMotion
import com.medownloader.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    isPremium: Boolean,
    wifiOnly: Boolean,
    maxConcurrent: Int,
    connectionLimit: Int,
    downloadPath: String?,
    onThemeSelected: (AppTheme) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onMaxConcurrentChanged: (Int) -> Unit,
    onConnectionLimitChanged: (Int) -> Unit,
    onDownloadPathClick: () -> Unit,
    onBackClick: () -> Unit,
    onGetProClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var appearanceExpanded by remember { mutableStateOf(false) }
    var downloadsExpanded by remember { mutableStateOf(true) }
    var performanceExpanded by remember { mutableStateOf(false) }
    var aboutExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isPremium) ProBanner(onGetProClick)

            ExpandableSection("appearance", Icons.Outlined.Palette, appearanceExpanded, { appearanceExpanded = !appearanceExpanded }) {
                ThemeSelector(currentTheme, isPremium, onThemeSelected)
            }

            ExpandableSection("downloads", Icons.Outlined.CloudDownload, downloadsExpanded, { downloadsExpanded = !downloadsExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingRow("download location", downloadPath ?: "tap to select", Icons.Outlined.FolderOpen, onDownloadPathClick)
                    SettingToggle("wi-fi only", "pause on mobile data", Icons.Outlined.Wifi, wifiOnly, onWifiOnlyChanged)
                }
            }

            ExpandableSection("performance", Icons.Outlined.Speed, performanceExpanded, { performanceExpanded = !performanceExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingSliderWithPreview("concurrent downloads", maxConcurrent, 1..12, if (isPremium) 12 else 4, onMaxConcurrentChanged, isPremium, onGetProClick)
                    SettingSliderWithPreview("connections per file", connectionLimit, 1..16, if (isPremium) 16 else 7, onConnectionLimitChanged, isPremium, onGetProClick)
                }
            }

            ExpandableSection("about", Icons.Outlined.Info, aboutExpanded, { aboutExpanded = !aboutExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingRow("meDownloader", "version 1.0.0", null, null)
                    SettingRow("by efaz", "made with love", null, null)
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(title: String, icon: ImageVector, expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(150), label = "chevron")

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null, Modifier.rotate(rotation), MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(expanded, enter = expandVertically(tween(150)) + fadeIn(tween(100)), exit = shrinkVertically(tween(150)) + fadeOut(tween(100))) {
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    HorizontalDivider(Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    content()
                }
            }
        }
    }
}

@Composable
fun ProBanner(onGetProClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onGetProClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(Shapes.small).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("upgrade to pro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("$3.99 lifetime • faster downloads • more themes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            TextButton(onClick = onGetProClick) { Text("get") }
        }
    }
}

@Composable
fun ThemeSelector(currentTheme: AppTheme, isPremium: Boolean, onThemeSelected: (AppTheme) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppTheme.entries.forEach { theme ->
            val isLocked = theme != AppTheme.DEFAULT && !isPremium
            ThemeOption(theme, currentTheme == theme, isLocked, if (!isLocked) { { onThemeSelected(theme) } } else null)
        }
    }
}

@Composable
fun ThemeOption(theme: AppTheme, isSelected: Boolean, isLocked: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val (name, colors) = when (theme) {
        AppTheme.DEFAULT -> "default" to listOf(Color(0xFF7C4DFF), Color(0xFFCE93D8), Color(0xFF84FFFF))
        AppTheme.CATPPUCCIN -> "catppuccin mocha" to listOf(Color(0xFFF5C2E7), Color(0xFFABE9B3), Color(0xFF89DCEB))
        AppTheme.GRUVBOX -> "gruvbox dark" to listOf(Color(0xFFFE8019), Color(0xFF8EC07C), Color(0xFFFABD2F))
        AppTheme.TOKYO_NIGHT -> "tokyo night" to listOf(Color(0xFF7AA2F7), Color(0xFF9ECE6A), Color(0xFFBB9AF7))
        AppTheme.NORD -> "nord" to listOf(Color(0xFF88C0D0), Color(0xFF81A1C1), Color(0xFFB48EAD))
    }

    Row(
        modifier = modifier.fillMaxWidth().clip(Shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(10.dp).alpha(if (isLocked) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // color preview dots
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            colors.forEach { c -> Box(Modifier.size(16.dp).clip(Shapes.extraSmall).background(c)) }
        }
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (isLocked) Icon(Icons.Default.Lock, "pro", Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        else if (isSelected) Icon(Icons.Default.Check, "selected", Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, icon: ImageVector?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SettingToggle(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingSliderWithPreview(
    title: String,
    value: Int,
    fullRange: IntRange,
    freeLimit: Int,
    onValueChange: (Int) -> Unit,
    isPremium: Boolean,
    onGetProClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (!isPremium && value >= freeLimit) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Lock, "pro limit", Modifier.size(12.dp).clickable(onClick = onGetProClick), MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        
        // visual slider showing full range but greyed out beyond limit
        Box(Modifier.fillMaxWidth()) {
            Slider(
                value = value.toFloat(),
                onValueChange = { 
                    val newVal = it.toInt()
                    if (isPremium || newVal <= freeLimit) onValueChange(newVal)
                    else onGetProClick()
                },
                valueRange = fullRange.first.toFloat()..fullRange.last.toFloat(),
                steps = fullRange.last - fullRange.first - 1,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (isPremium) MaterialTheme.colorScheme.surfaceContainerHighest 
                        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                )
            )
            
            // overlay showing locked portion
            if (!isPremium) {
                val lockedStart = (freeLimit - fullRange.first).toFloat() / (fullRange.last - fullRange.first)
                Box(
                    Modifier.fillMaxWidth(1f - lockedStart).align(Alignment.CenterEnd).height(4.dp)
                        .padding(end = 12.dp).clip(Shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
            }
        }
        
        if (!isPremium) {
            Text("unlock up to ${fullRange.last} with pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onGetProClick))
        }
    }
}
