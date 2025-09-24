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
package cloud.dmytrominochkin.ai.llamacompose.simple

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cloud.dmytrominochkin.ai.llamacompose.components.ChatScreenScaffold
import cloud.dmytrominochkin.ai.llamacompose.components.MarkdownCard
import cloud.dmytrominochkin.ai.llamacompose.components.UnifiedMessageCard
import cloud.dmytrominochkin.ai.llamacompose.components.VerticalScrollbar
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_assistant_label
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_input_placeholder
import cloud.dmytrominochkin.ai.llamacompose.resources.simple_chat_content
import cloud.dmytrominochkin.ai.llamacompose.resources.simple_chat_title
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import org.jetbrains.compose.resources.stringResource

@Composable
fun SimpleChatScreen(
    chatViewModel: SimpleChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by chatViewModel.uiState.collectAsState()

    ChatScreenScaffold(
        messages = uiState.messages,
        isGenerating = uiState.isGenerating,
        streamingAssistantContent = uiState.streamingAssistantContent,
        currentMessage = uiState.currentMessage,
        contextUsagePercent = uiState.contextUsagePercent,
        inputPlaceholder = stringResource(Res.string.chat_input_placeholder),
        onMessageChange = { chatViewModel.updateMessage(it) },
        onSend = { chatViewModel.send() },
        onStop = { chatViewModel.stop() },
        onClear = { chatViewModel.clear() },
        messageCard = { message, isMsgGenerating, onCopy, onPosition ->
            UnifiedMessageCard(
                message = message,
                assistantLabel = stringResource(Res.string.chat_assistant_label),
                isGenerating = isMsgGenerating,
                generatingAnimationFile = "think.json",
                generatingAnimationSize = AppDimension.animationSizeS,
                onCopyClick = onCopy,
                onPosition = onPosition,
            )
        },
        emptyHeader = {
            MarkdownCard(
                title = stringResource(Res.string.simple_chat_title),
                markdown = stringResource(Res.string.simple_chat_content),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimension.spacingS)
            )
        },
        isTerminated = uiState.isChatEnded,
        modifier = modifier
    )
}
