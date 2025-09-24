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

import ai.koog.agents.core.exception.AgentRuntimeException
import ai.koog.agents.core.exception.UnexpectedServerException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.dmytrominochkin.ai.llamacompose.agent.client.LlamaLLMClient
import cloud.dmytrominochkin.ai.llamacompose.agent.client.LlamaLLMExecutor
import cloud.dmytrominochkin.ai.llamacompose.components.UiMessage
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaModel
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.error_generation
import cloud.dmytrominochkin.ai.llamacompose.resources.error_response_format
import cloud.dmytrominochkin.ai.llamacompose.resources.error_response_processing
import cloud.dmytrominochkin.ai.llamacompose.resources.error_response_tool_unavailable
import cloud.dmytrominochkin.ai.llamacompose.resources.error_response_unknown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import kotlin.math.roundToInt

data class AgentChatUiState(
    val isGenerating: Boolean = false,
    val contextUsagePercent: Int = 0,

    val currentMessage: String = "",
    val messages: List<UiMessage> = emptyList(),

    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null,
    val isChatEnded: Boolean = false,
)

class AgentChatViewModel(
    private val llama: LlamaModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentChatUiState())

    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    fun updateMessage(text: String) {
        _uiState.update { it.copy(currentMessage = text) }
    }

    fun send() {
        val text = _uiState.value.currentMessage
        if (text.isBlank()) return
        if (_uiState.value.isChatEnded) return

        // If agent is waiting for a clarifying response, capture user's reply and resume agent
        if (_uiState.value.userResponseRequested) {
            _uiState.update {
                it.copy(
                    currentMessage = "",
                    messages = it.messages + UiMessage.User(text),
                    currentUserResponse = text,
                    userResponseRequested = false,
                    isGenerating = true,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                currentMessage = "",
                messages = it.messages + UiMessage.User(text),
                contextUsagePercent = 0,
                isGenerating = true,
            )
        }

        generationJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val agent = createAgent(
                        promptExecutor = {
                            LlamaLLMExecutor(LlamaLLMClient(llama))
                        },
                        onToolCall = { message ->
                            _uiState.update { it.copy(messages = it.messages + UiMessage.Events(message)) }
                        },
                        onToolCallFailure = { message ->
                            _uiState.update { it.copy(messages = it.messages + UiMessage.Events(message)) }
                        },
                        onToolCallResult = { message ->
                            _uiState.update { it.copy(messages = it.messages + UiMessage.Events(message)) }
                        },
                        onBeforeLLMCall = { message ->
                            _uiState.update { it.copy(messages = it.messages + UiMessage.Events(message)) }
                        },
                        onAfterLLMCall = {
                            val percent = llama.getContextUsagePercent()
                            val pct = percent.roundToInt().coerceIn(0, 100)
                            _uiState.update {
                                it.copy(
                                    contextUsagePercent = pct
                                )
                            }
                            // If context is critically high, end chat and cancel any running job
                            if (pct >= 90) {
                                _uiState.update { it.copy(isChatEnded = true) }
                                stop()
                            }
                        },
                        onAgentRunError = { errorMessage ->
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + UiMessage.Events(errorMessage),
                                    isGenerating = false
                                )
                            }
                        },
                        onAssistantMessage = { message ->
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + UiMessage.Assistant(message),
                                    isGenerating = false,
                                    userResponseRequested = true
                                )
                            }

                            val userResponse = uiState.first { it.currentUserResponse != null }.currentUserResponse ?: ""

                            // Reset the captured response
                            _uiState.update { it.copy(currentUserResponse = null) }

                            userResponse
                        }
                    )

                    val result = agent.run(text)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + UiMessage.Assistant(result.ifBlank { "👋" }),
                            isChatEnded = true
                        )
                    }
                } catch (e: Exception) {
                    val errorMessage = when (e) {
                        is UnexpectedServerException -> {
                            when {
                                e.message?.contains("JsonLiteral is not a JsonObject") == true ->
                                    getString(Res.string.error_response_format)

                                e.message?.contains("is not defined") == true ->
                                    getString(Res.string.error_response_tool_unavailable)

                                else -> getString(
                                    Res.string.error_generation,
                                    e.message ?: getString(Res.string.error_response_unknown)
                                )
                            }
                        }

                        is AgentRuntimeException -> {
                            getString(Res.string.error_response_processing)
                        }

                        else -> getString(
                            Res.string.error_generation,
                            e.message ?: getString(Res.string.error_response_unknown)
                        )
                    }
                    _uiState.update {
                        it.copy(
                            messages = it.messages + UiMessage.Assistant(
                                errorMessage
                            )
                        )
                    }
                } finally {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            userResponseRequested = false,
                            currentUserResponse = null,
                        )
                    }
                    generationJob = null
                    runCatching {
                        val percent = llama.getContextUsagePercent()
                        val pct = percent.roundToInt().coerceIn(0, 100)
                        _uiState.update {
                            it.copy(
                                contextUsagePercent = pct,
                                isChatEnded = it.isChatEnded || pct >= 90
                            )
                        }
                    }.onFailure {
                        // TODO: logging
                    }
                }
            }
        }
    }

    fun stop() {
        runCatching { llama.requestCancel() }
        generationJob?.cancel()
        generationJob = null
        _uiState.update {
            it.copy(
                isGenerating = false
            )
        }
    }

    fun clear() {
        runCatching { llama.requestCancel() }
        generationJob?.cancel()
        generationJob = null
        llama.clearConversation()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                contextUsagePercent = 0,
                isChatEnded = false
            )
        }
    }
}
