package com.medownloader.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medownloader.data.repository.FileInfo
import com.medownloader.ui.theme.BottomSheetShape
import com.medownloader.ui.theme.CardShape
import com.medownloader.ui.theme.ExpressiveMotion
import com.medownloader.ui.theme.InputFieldShape
import com.medownloader.ui.theme.MonoTextStyle
import com.medownloader.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadSheet(
    fileInfo: com.medownloader.data.repository.FileInfo?,
    isLoading: Boolean,
    pendingUrl: String?,
    onFetchInfo: (String) -> Unit,
    onConfirmAdd: (url: String, filename: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    val view = LocalView.current

    var url by remember { mutableStateOf(pendingUrl ?: "") }
    var filename by remember { mutableStateOf("") }

    LaunchedEffect(fileInfo) {
        fileInfo?.let {
            filename = it.filename
        }
    }

    LaunchedEffect(pendingUrl) {
        pendingUrl?.let { url = it }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(Shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .animateContentSize()
        ) {
            Text(
                text = "new download",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("url") },
                placeholder = { Text("paste download link") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        clipboardManager.getText()?.text?.let {
                            url = it
                            onFetchInfo(it)
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "paste")
                    }
                },
                singleLine = true,
                shape = InputFieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "fetching file info...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = fileInfo != null && !isLoading,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                fileInfo?.let { info ->
                    FileInfoCard(
                        filename = filename,
                        size = info.size ?: 0L,
                        mimeType = info.mimeType,
                        onFilenameChange = { filename = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = ExpressiveMotion.MicroBounce,
                label = "buttonScale"
            )

            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    if (url.isNotBlank()) {
                        onConfirmAdd(url, filename.ifBlank { "download" })
                    }
                },
                enabled = url.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(scale),
                shape = Shapes.medium,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "start download",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FileInfoCard(
    filename: String,
    size: Long,
    mimeType: String?,
    onFilenameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = filename,
                onValueChange = onFilenameChange,
                label = { Text("filename") },
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                singleLine = true,
                shape = InputFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Outlined.Storage,
                    label = formatBytes(size)
                )
                mimeType?.let {
                    InfoChip(
                        icon = Icons.Outlined.Description,
                        label = it.substringAfter("/")
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(Shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MonoTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f gb".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f mb".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f kb".format(bytes / 1_000.0)
    else -> "$bytes b"
}
