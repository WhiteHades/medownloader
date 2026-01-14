package com.medownloader.presentation.screen
import com.medownloader.util.formatSize
import androidx.compose.ui.res.stringResource

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medownloader.data.repository.FileInfo
import com.medownloader.ui.theme.*

import com.medownloader.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddDownloadSheet(
    fileInfo: FileInfo?,
    isLoading: Boolean,
    pendingUrl: String?,
    onFetchInfo: (String) -> Unit,
    onConfirmAdd: (url: String, filename: String?) -> Unit,
    onDismiss: () -> Unit,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    
    // Local URL state, initialized from pendingUrl
    var url by remember(pendingUrl) { mutableStateOf(pendingUrl ?: "") }
    
    // Auto-focus URL field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Sheet content with spring animation
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ExpressiveShapeTokens.BottomSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(width = 40.dp, height = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = ExpressiveShapeTokens.Full
                ) {}
            }
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_download_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.add_download_close_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // URL Input with paste button
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text(stringResource(R.string.add_download_url_label)) },
                placeholder = { Text(stringResource(R.string.add_download_url_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = if (url.isNotEmpty() && isValidUrl(url))
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    Row {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { url = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.add_download_clear_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { pastedUrl ->
                                url = pastedUrl
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                        }) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = stringResource(R.string.add_download_paste_desc),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (url.isNotEmpty() && isValidUrl(url)) {
                            if (fileInfo != null) {
                                onConfirmAdd(url, fileInfo.filename)
                            } else {
                                onFetchInfo(url)
                            }
                        }
                    }
                ),
                shape = ExpressiveShapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
            
            // URL validation indicator
            AnimatedVisibility(
                visible = url.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (isValidUrl(url)) {
                    UrlValidCard(url = url)
                } else {
                    UrlInvalidCard()
                }
            }
            
            // File info preview (when available)
            AnimatedVisibility(
                visible = fileInfo != null,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + 
                        expandVertically(spring(dampingRatio = 0.7f)),
                exit = fadeOut() + shrinkVertically()
            ) {
                fileInfo?.let { info ->
                    FileInfoCard(info = info, isPremium = isPremium)
                }
            }
            
            // Loading state
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                LoadingCard()
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action button - Fetch info or confirm download
            if (fileInfo != null) {
                ExpressiveDownloadButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onConfirmAdd(url, fileInfo.filename)
                    },
                    enabled = !isLoading,
                    text = stringResource(R.string.add_download_start)
                )
            } else {
                ExpressiveDownloadButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        if (url.isNotEmpty() && isValidUrl(url)) {
                            onFetchInfo(url)
                        }
                    },
                    enabled = url.isNotEmpty() && isValidUrl(url) && !isLoading,
                    text = stringResource(R.string.add_download_add)
                )
            }
        }
    }
}

@Composable
private fun UrlValidCard(url: String) {
    Surface(
        color = SuccessGreen90.copy(alpha = 0.5f),
        shape = ExpressiveShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SuccessGreen40
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.add_download_valid_url),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SuccessGreen30
                )
                Text(
                    text = extractDomain(url),
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen40,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UrlInvalidCard() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = ExpressiveShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.add_download_invalid_url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun FileInfoCard(info: FileInfo, isPremium: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = ExpressiveShapeTokens.CardSoft
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.filename,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = info.mimeType ?: stringResource(R.string.add_download_unknown_type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                info.size?.let { size ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = ExpressiveShapeTokens.Full
                    ) {
                        Text(
                            text = formatSize(size),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MonoTextStyleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Resume support indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (info.resumable) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (info.resumable) SuccessGreen40 else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (info.resumable) stringResource(R.string.add_download_resume_supported) else stringResource(R.string.add_download_resume_not_supported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Speed boost hint for non-premium
            val fileSize = info.size ?: 0L
            if (!isPremium && fileSize > 100 * 1024 * 1024) { // > 100MB
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = ExpressiveShapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = stringResource(R.string.add_download_large_file_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = ExpressiveShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.add_download_fetching),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveDownloadButton(
    onClick: () -> Unit,
    enabled: Boolean,
    text: String = "Start Download"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveMotion.MicroBounce,
        label = "buttonScale"
    )
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        enabled = enabled,
        interactionSource = interactionSource,
        // M3 Expressive: Use shapes parameter for morphing shape support
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Icon(
            Icons.Filled.Download,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Utility functions
private fun isValidUrl(url: String): Boolean {
    return url.startsWith("http://") || url.startsWith("https://") ||
           url.startsWith("ftp://") || url.startsWith("magnet:")
}

private fun extractDomain(url: String): String {
    return try {
        val cleaned = url.removePrefix("https://").removePrefix("http://").removePrefix("ftp://")
        cleaned.substringBefore("/").substringBefore("?")
    } catch (e: Exception) {
        url
    }
}
