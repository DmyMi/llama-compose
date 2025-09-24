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
package cloud.dmytrominochkin.ai.llamacompose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_send
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_stop
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_context_used
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_message_copied
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_start_new
import cloud.dmytrominochkin.ai.llamacompose.resources.gauge_percentage
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource


@Composable
fun ChatScreenScaffold(
    messages: List<UiMessage>,
    isGenerating: Boolean,
    streamingAssistantContent: String,
    currentMessage: String,
    contextUsagePercent: Int,
    inputPlaceholder: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    messageCard: @Composable (
        message: UiMessage,
        isGenerating: Boolean,
        onCopyClick: () -> Unit,
        onPosition: (IntSize) -> Unit,
    ) -> Unit,
    emptyHeader: (@Composable () -> Unit)? = null,
    isTerminated: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberLazyListState()
    var isCopyVisible by remember { mutableStateOf(false) }
    var lastItemSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-scroll
    LaunchedEffect(
        messages.size,
        messages.lastOrNull()?.content,
        isGenerating,
        streamingAssistantContent,
    ) {
        if (messages.isNotEmpty()) {
            val offset = if (isGenerating) {
                val itemHeightPx = lastItemSize.height
                val viewportHeightPx = scrollState.layoutInfo.viewportSize.height
                (itemHeightPx - viewportHeightPx).coerceAtLeast(0)
            } else {
                0
            }
            val targetIndex = if (isGenerating) messages.lastIndex + 1 else messages.lastIndex

            // Need to wait a bit while the markdown re-draws
            if (!isGenerating && streamingAssistantContent.isBlank()) delay(100)
            scrollState.animateScrollToItem(index = targetIndex, scrollOffset = offset)
        }
    }

    // Auto-hide copy popup after 2 seconds
    LaunchedEffect(isCopyVisible) {
        if (isCopyVisible) {
            delay(2000)
            isCopyVisible = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Surface(
                tonalElevation = AppDimension.elevationS,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(AppDimension.elevationS),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimension.spacingS),
                    state = scrollState
                ) {
                    if (messages.isEmpty() && emptyHeader != null) {
                        item { emptyHeader() }
                    }

                    items(messages) { message ->
                        val isLastAssistantMessage = messages.lastOrNull() == message && message is UiMessage.Assistant
                        messageCard(
                            message,
                            isGenerating && isLastAssistantMessage,
                            { isCopyVisible = true },
                            {}
                        )
                    }

                    // Static generating item
                    if (isGenerating) {
                        item {
                            messageCard(
                                UiMessage.Assistant(streamingAssistantContent),
                                true,
                                { isCopyVisible = true },
                                { lastItemSize = it }
                            )
                        }
                    }
                }
            }

            VerticalScrollbar(scrollState)

            if (isCopyVisible) {
                Popup(
                    alignment = Alignment.BottomCenter,
                    onDismissRequest = { isCopyVisible = false }
                ) {
                    ElevatedCard(
                        modifier = Modifier.padding(AppDimension.spacingS),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppDimension.spacingS),
                    ) {
                        Text(
                            stringResource(Res.string.chat_message_copied),
                            modifier = Modifier.padding(AppDimension.spacingXS)
                        )
                    }
                }
            }
        }

        // Input area or end-of-run CTA
        AnimatedContent(
            targetState = isTerminated,
            transitionSpec = { fadeIn(tween(650)) togetherWith fadeOut(tween(650)) },
            label = "agent-bottom-bar"
        ) { ended ->
            if (ended) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimension.spacingS)
                ) {
                    ContextControls(contextUsagePercent, onClear, Modifier.fillMaxWidth(), !isGenerating)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimension.spacingS)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentMessage,
                            onValueChange = onMessageChange,
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            label = { Text(inputPlaceholder) },
                            enabled = !isGenerating,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            singleLine = true,
                        )
                        FilledIconButton(
                            onClick = { if (isGenerating) onStop() else onSend() },
                            enabled = isGenerating || currentMessage.isNotBlank(),
                            modifier = Modifier.size(AppDimension.iconButtonSizeL)
                        ) {
                            Icon(
                                if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (isGenerating) stringResource(Res.string.cd_stop) else stringResource(
                                    Res.string.cd_send
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimension.spacingXS))

                    ContextControls(contextUsagePercent, onClear, Modifier.fillMaxWidth(), !isGenerating)
                }
            }
        }
    }
}

@Composable
private fun ContextControls(
    contextUsagePercent: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    buttonEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingS)
        ) {
            Gauge(
                value = contextUsagePercent,
                minValue = 0,
                maxValue = 100,
                modifier = Modifier.width(AppDimension.spacingXXL * 2),
                config = DialConfig(displayScale = false),
                mainLabel = { value -> Text(stringResource(Res.string.gauge_percentage, value)) },
                minAndMaxValueLabel = {}
            )
            Text(
                text = stringResource(Res.string.chat_context_used),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onClear,
            enabled = buttonEnabled,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text(stringResource(Res.string.chat_start_new))
        }
    }
}
