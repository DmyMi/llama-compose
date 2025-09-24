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
package cloud.dmytrominochkin.ai.llamacompose.agent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cloud.dmytrominochkin.ai.llamacompose.components.ChatScreenScaffold
import cloud.dmytrominochkin.ai.llamacompose.components.MarkdownCard
import cloud.dmytrominochkin.ai.llamacompose.components.ModelSelectHint
import cloud.dmytrominochkin.ai.llamacompose.components.UnifiedMessageCard
import cloud.dmytrominochkin.ai.llamacompose.main.MainViewModel
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.agent_features_content
import cloud.dmytrominochkin.ai.llamacompose.resources.agent_input_awaiting_user
import cloud.dmytrominochkin.ai.llamacompose.resources.agent_title
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_agent_input_placeholder
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_agent_label
import cloud.dmytrominochkin.ai.llamacompose.resources.show_agent_state
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AgentChatScreenWrapper(
    mainViewModel: MainViewModel,
    showAppState: () -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val agentChatViewModel: AgentChatViewModel = koinViewModel()

    if (uiState.isModelLoaded) {
        Box {
            Column {
                OutlinedButton(
                    onClick = { showAppState() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(Res.string.show_agent_state))
                }
                AgentChatScreen(chatViewModel = agentChatViewModel)
            }
        }
    } else {
        ModelSelectHint(
            onSelectModelClick = { mainViewModel.toggleModelDownloadSheet() }
        )
    }
}

@Composable
private fun AgentChatScreen(chatViewModel: AgentChatViewModel) {
    val uiState by chatViewModel.uiState.collectAsState()

    ChatScreenScaffold(
        messages = uiState.messages,
        isGenerating = uiState.isGenerating,
        streamingAssistantContent = "",
        currentMessage = uiState.currentMessage,
        contextUsagePercent = uiState.contextUsagePercent,
        inputPlaceholder = if (uiState.userResponseRequested)
            stringResource(Res.string.agent_input_awaiting_user)
        else
            stringResource(Res.string.chat_agent_input_placeholder),
        onMessageChange = { chatViewModel.updateMessage(it) },
        onSend = { chatViewModel.send() },
        onStop = { chatViewModel.stop() },
        onClear = { chatViewModel.clear() },
        messageCard = { message, isMsgGenerating, onCopy, onPosition ->
            UnifiedMessageCard(
                message = message,
                assistantLabel = stringResource(Res.string.chat_agent_label),
                isGenerating = isMsgGenerating,
                generatingAnimationFile = "ai.json",
                generatingAnimationSize = AppDimension.animationSizeM,
                onCopyClick = onCopy,
                onPosition = onPosition
            )
        },
        emptyHeader = {
            MarkdownCard(
                title = stringResource(Res.string.agent_title),
                markdown = stringResource(Res.string.agent_features_content),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimension.spacingS)
            )
        },
        isTerminated = uiState.isChatEnded,
    )
}
