/*
   Copyright 2025 Dmytro Minochkin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package cloud.dmytrominochkin.ai.llamacompose.download

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.unit.dp
import cloud.dmytrominochkin.ai.llamacompose.components.AnimatedTabHeader
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownPadding
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownTypography
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.download_cancel
import cloud.dmytrominochkin.ai.llamacompose.resources.download_completed_message
import cloud.dmytrominochkin.ai.llamacompose.resources.download_delete
import cloud.dmytrominochkin.ai.llamacompose.resources.download_delete_confirm_message
import cloud.dmytrominochkin.ai.llamacompose.resources.download_delete_confirm_title
import cloud.dmytrominochkin.ai.llamacompose.resources.download_description
import cloud.dmytrominochkin.ai.llamacompose.resources.download_download
import cloud.dmytrominochkin.ai.llamacompose.resources.download_failed_message
import cloud.dmytrominochkin.ai.llamacompose.resources.download_load
import cloud.dmytrominochkin.ai.llamacompose.resources.download_retry
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_all
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_compact
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_desktop
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_mobile
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_subtitle_all
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_subtitle_compact
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_subtitle_desktop
import cloud.dmytrominochkin.ai.llamacompose.resources.filter_subtitle_mobile
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

enum class ModelFilter { ALL, DESKTOP, MOBILE, COMPACT }

@Composable
fun ModelDownloadScreen(
    viewModel: ModelDownloadViewModel,
    snackbarHostState: SnackbarHostState,
    onLoadClicked: (String, String) -> Unit,
    loadedModelName: String,
    isModelLoaded: Boolean,
) {
    val models by viewModel.models.collectAsState()
    var selectedFilter by remember { mutableStateOf(ModelFilter.ALL) }

    val filteredModels = remember(models, selectedFilter) {
        when (selectedFilter) {
            ModelFilter.ALL -> models
            ModelFilter.DESKTOP -> models.filter { it.model.category == ModelCategory.DESKTOP }
            ModelFilter.MOBILE -> models.filter { it.model.category == ModelCategory.MOBILE }
            ModelFilter.COMPACT -> models.filter { it.model.category == ModelCategory.COMPACT }
        }
    }

    // Track previous model states to detect download completion
    val previousStates = remember { mutableStateMapOf<String, ModelStatus>() }
    val coroutineScope = rememberCoroutineScope()
    val previousStatusMessages = remember { mutableStateMapOf<String, String?>() }

    // Monitor for download completion and show snackbar
    LaunchedEffect(models) {
        models.forEach { uiState ->
            val modelKey = uiState.model.filename
            val currentStatus = uiState.status
            val previousStatus = previousStates[modelKey]

            if (previousStatus == ModelStatus.DOWNLOADING && currentStatus == ModelStatus.DOWNLOADED) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.download_completed_message))
                }
            }

            if (previousStatus != ModelStatus.ERROR && currentStatus == ModelStatus.ERROR) {
                val message = uiState.errorMessage ?: getString(Res.string.download_failed_message)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }

            val currentMsg = uiState.statusMessage
            val prevMsg = previousStatusMessages[modelKey]
            if (currentMsg != null && currentMsg != prevMsg) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(currentMsg)
                }
            }

            // Update previous state
            previousStates[modelKey] = currentStatus
            previousStatusMessages[modelKey] = currentMsg
        }
    }

    Surface(
        tonalElevation = AppDimension.elevationS,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(AppDimension.elevationS),
        modifier = Modifier.keepScreenOn()
    ) {
        Column(Modifier.fillMaxSize().padding(AppDimension.spacingM)) {
            val tabs = listOf(
                stringResource(Res.string.filter_all),
                stringResource(Res.string.filter_desktop),
                stringResource(Res.string.filter_mobile),
                stringResource(Res.string.filter_compact),
            )
            val filterValues = listOf(
                ModelFilter.ALL,
                ModelFilter.DESKTOP,
                ModelFilter.MOBILE,
                ModelFilter.COMPACT,
            )
            val selectedIndex = filterValues.indexOf(selectedFilter).coerceAtLeast(0)
            
            AnimatedTabHeader(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelected = { index -> selectedFilter = filterValues[index] }
            )
            Spacer(Modifier.height(AppDimension.spacingS))
            Text(
                text = when (selectedFilter) {
                    ModelFilter.ALL -> stringResource(Res.string.filter_subtitle_all)
                    ModelFilter.DESKTOP -> stringResource(Res.string.filter_subtitle_desktop)
                    ModelFilter.MOBILE -> stringResource(Res.string.filter_subtitle_mobile)
                    ModelFilter.COMPACT -> stringResource(Res.string.filter_subtitle_compact)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(AppDimension.spacingS))

            LazyColumn(Modifier.fillMaxSize()) {
                items(filteredModels, key = { it.model.filename }) { uiState ->
                    ModelRow(
                        uiState = uiState,
                        onDownload = { viewModel.download(uiState.model) },
                        onCancel = { viewModel.cancel(uiState.model) },
                        onLoad = {
                            viewModel.localPath(uiState.model)?.let { path ->
                                onLoadClicked(path, uiState.model.name)
                            }
                        },
                        onDeleteConfirmed = { viewModel.delete(uiState.model) },
                        isAnyModelLoaded = isModelLoaded,
                        isLoadedModel = uiState.model.name == loadedModelName,
                    )
                    Spacer(Modifier.height(AppDimension.spacingS))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelRow(
    uiState: ModelUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onLoad: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    isAnyModelLoaded: Boolean,
    isLoadedModel: Boolean,
) {
    var showConfirm by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expandIconRotation"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(AppDimension.spacingS)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = uiState.model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(AppDimension.iconButtonSizeXS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(rotationState)
                        )
                    }
                }

                Spacer(Modifier.height(AppDimension.spacingXS))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingS)
                ) {
                    uiState.model.sizeReadable?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.model.quantization.isNotEmpty()) {
                        Text(
                            text = uiState.model.quantization,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = uiState.model.capabilities,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(AppDimension.spacingS))

                Row(horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingS), verticalAlignment = Alignment.CenterVertically) {
                    when (uiState.status) {
                        ModelStatus.READY -> {
                            if (uiState.statusMessage != null) {
                                Button(onClick = {}, enabled = false) { Text(uiState.statusMessage) }
                                TextButton(onClick = onCancel) { Text(stringResource(Res.string.download_cancel)) }
                            } else {
                                Button(onClick = onDownload) { Text(stringResource(Res.string.download_download)) }
                            }
                            if (uiState.hasPartial) {
                                TextButton(onClick = { showConfirm = true }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(Res.string.download_delete))
                                }
                            }
                        }
                        ModelStatus.DOWNLOADING -> {
                            Button(onClick = onCancel) { Text(stringResource(Res.string.download_cancel)) }
                            if (uiState.bytesPerSecond != null || uiState.etaSeconds != null) {
                                Spacer(Modifier.width(8.dp))
                                val speedStr = uiState.bytesPerSecond?.let { formatBytesPerSecond(it) }
                                val etaStr = uiState.etaSeconds?.let { formatEtaSeconds(it) }
                                val text = listOfNotNull(speedStr, etaStr).joinToString(" · ")
                                if (text.isNotEmpty()) {
                                    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        ModelStatus.DOWNLOADED -> {
                            Button(onClick = onLoad, enabled = !isAnyModelLoaded) { Text(stringResource(Res.string.download_load)) }
                            TextButton(onClick = { showConfirm = true }, enabled = !isLoadedModel) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(AppDimension.spacingXS))
                                Text(stringResource(Res.string.download_delete))
                            }
                        }
                        ModelStatus.ERROR -> {
                            Button(onClick = onDownload) { Text(stringResource(Res.string.download_retry)) }
                            uiState.errorMessage?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (uiState.status == ModelStatus.DOWNLOADING) {
                    Spacer(Modifier.height(AppDimension.spacingS))
                    if (uiState.progressIndeterminate) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { uiState.progress.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(Modifier.height(AppDimension.spacingS))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppDimension.spacingS)
                    ) {
                        if (uiState.model.description.isNotEmpty()) {
                            Text(
                                text = stringResource(Res.string.download_description),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Markdown(
                                content = uiState.model.description,
                                typography = appMarkdownTypography(text = MaterialTheme.typography.labelSmall),
                                padding = appMarkdownPadding(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(Res.string.download_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.download_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDeleteConfirmed()
                }) { Text(stringResource(Res.string.download_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(Res.string.download_cancel)) }
            }
        )
    }
}

private fun formatBytesPerSecond(bps: Long): String {
    val units = listOf("B/s", "KB/s", "MB/s", "GB/s")
    var value = bps.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    val roundedStr = if (value >= 100) {
        value.roundToInt().toString()
    } else {
        val oneDecimal = (value * 10.0).roundToInt() / 10.0
        oneDecimal.toString()
    }
    return "$roundedStr ${units[unitIndex]}"
}

private fun formatEtaSeconds(seconds: Long): String {
    val s = seconds % 60
    val m = (seconds / 60) % 60
    val h = seconds / 3600
    val sStr = s.toString().padStart(2, '0')
    val mStr = m.toString().padStart(2, '0')
    return when {
        h > 0 -> "${h}h ${mStr}m ${sStr}s left"
        m > 0 -> "${m}m ${sStr}s left"
        else -> "${s}s left"
    }
}
