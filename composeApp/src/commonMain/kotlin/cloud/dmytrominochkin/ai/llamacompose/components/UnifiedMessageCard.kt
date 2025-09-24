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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_events_label
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_user_label
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.stringResource

sealed interface UiMessage {
    val content: String

    data class User(override val content: String) : UiMessage

    data class Assistant(override val content: String) : UiMessage

    data class Events(override val content: String) : UiMessage
}

@Composable
fun UserMessageCard(
    message: UiMessage.User,
    onCopyClick: () -> Unit = {},
    onPosition: (IntSize) -> Unit = {},
) {
    // Though deprecated, but it works in common code
    val clipboard = LocalClipboardManager.current
    BoxWithConstraints(
        modifier = Modifier.onGloballyPositioned { onPosition(it.size) }
    ) {
        val thirdScreenWidth = maxWidth / 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = thirdScreenWidth * 2)
                    .padding(vertical = AppDimension.spacingXS),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(AppDimension.spacingM)) {
                    Text(
                        text = stringResource(Res.string.chat_user_label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(AppDimension.spacingXS))

                    Markdown(
                        content = message.content,
                        typography = appMarkdownTypography(),
                        padding = appMarkdownPadding(),
                        modifier = Modifier.clickable {
                            clipboard.setText(AnnotatedString(message.content))
                            onCopyClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantMessageCard(
    message: UiMessage.Assistant,
    assistantLabel: String,
    isGenerating: Boolean = false,
    onCopyClick: () -> Unit = {},
    onPosition: (IntSize) -> Unit = {},
    generatingAnimationFile: String = "think.json",
    generatingAnimationSize: Dp = 80.dp,
) {
    // Though deprecated, but it works in common code
    val clipboard = LocalClipboardManager.current
    BoxWithConstraints(
        modifier = Modifier.onGloballyPositioned { onPosition(it.size) }
    ) {
        val thirdScreenWidth = maxWidth / 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = thirdScreenWidth * 2)
                    .padding(vertical = AppDimension.spacingXS),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(AppDimension.spacingM)) {
                    Text(
                        text = assistantLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(AppDimension.spacingXS))

                    when {
                        message.content.isEmpty() && isGenerating -> {
                            KottieAnimationCard(
                                animationFile = generatingAnimationFile,
                                text = null,
                                animationSize = generatingAnimationSize,
                                modifier = Modifier.wrapContentSize()
                            )
                        }

                        isGenerating -> {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        else -> {
                            Markdown(
                                content = message.content,
                                typography = appMarkdownTypography(),
                                padding = appMarkdownPadding(),
                                modifier = Modifier.clickable {
                                    clipboard.setText(AnnotatedString(message.content))
                                    onCopyClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventMessageCard(
    message: UiMessage.Events,
    onPosition: (IntSize) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = Modifier.onGloballyPositioned { onPosition(it.size) }
    ) {
        val thirdScreenWidth = maxWidth / 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = thirdScreenWidth * 2)
                    .padding(vertical = AppDimension.spacingXS),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(AppDimension.spacingM)) {
                    Text(
                        text = stringResource(Res.string.chat_events_label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(AppDimension.spacingXS))

                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedMessageCard(
    message: UiMessage,
    assistantLabel: String,
    isGenerating: Boolean = false,
    onCopyClick: () -> Unit = {},
    onPosition: (IntSize) -> Unit = {},
    generatingAnimationFile: String = "think.json",
    generatingAnimationSize: Dp = AppDimension.animationSizeS
) {
    when (message) {
        is UiMessage.User -> UserMessageCard(
            message = message,
            onCopyClick = onCopyClick,
            onPosition = onPosition,
        )

        is UiMessage.Assistant -> AssistantMessageCard(
            message = message,
            assistantLabel = assistantLabel,
            isGenerating = isGenerating,
            onCopyClick = onCopyClick,
            onPosition = onPosition,
            generatingAnimationFile = generatingAnimationFile,
            generatingAnimationSize = generatingAnimationSize,
        )

        is UiMessage.Events -> EventMessageCard(
            message = message,
            onPosition = onPosition,
        )
    }
}
