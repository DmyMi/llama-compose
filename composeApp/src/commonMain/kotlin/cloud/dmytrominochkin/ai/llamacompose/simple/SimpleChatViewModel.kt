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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.dmytrominochkin.ai.llamacompose.components.UiMessage
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaModel
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.chat_generation_stopped
import cloud.dmytrominochkin.ai.llamacompose.resources.error_generation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.math.roundToInt

data class SimpleChatUiState(
    val isGenerating: Boolean = false,
    val contextUsagePercent: Int = 0,

    val currentMessage: String = "",
    val messages: List<UiMessage> = emptyList(),
    val streamingAssistantContent: String = "",
    val isChatEnded: Boolean = false,
)

class SimpleChatViewModel(private val llama: LlamaModel) : ViewModel() {

    private val _uiState = MutableStateFlow(SimpleChatUiState())

    val uiState: StateFlow<SimpleChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    fun updateMessage(text: String) {
        _uiState.update { it.copy(currentMessage = text) }
    }

    fun send() {
        val text = _uiState.value.currentMessage
        if (text.isBlank()) return
        if (_uiState.value.isChatEnded) return

        _uiState.update {
            it.copy(
                currentMessage = "",
                messages = it.messages + UiMessage.User(text)
            )
        }

        generationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    streamingAssistantContent = ""
                )
            }
            try {
                val responseFlow = llama.continueConversation(text, 512)

                responseFlow.collect { responseChunk ->
                    _uiState.update {
                        it.copy(
                            streamingAssistantContent = it.streamingAssistantContent + responseChunk
                        )
                    }
                }

                val finalContent = _uiState.value.streamingAssistantContent
                if (finalContent.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            messages = it.messages + UiMessage.Assistant(finalContent)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + UiMessage.Assistant(
                            getString(Res.string.error_generation, e.message ?: "")
                        )
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        streamingAssistantContent = ""
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

    fun stop() {
        runCatching { llama.requestCancel() }
        generationJob?.cancel()
        generationJob = null

        val partial = _uiState.value.streamingAssistantContent
        if (partial.isNotEmpty()) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        messages = it.messages + UiMessage.Assistant(
                            getString(Res.string.chat_generation_stopped, partial)
                        )
                    )
                }
            }
        }

        _uiState.update {
            it.copy(
                isGenerating = false,
                streamingAssistantContent = ""
            )
        }
    }

    fun clear() {
        val job = generationJob
        if (job != null) {
            runCatching { llama.requestCancel() }
            viewModelScope.launch {
                runCatching { job.join() }
                llama.clearConversation()
            }
        } else {
            llama.clearConversation()
        }
        _uiState.update {
            it.copy(
                isGenerating = false,
                messages = emptyList(),
                streamingAssistantContent = "",
                contextUsagePercent = 0,
                isChatEnded = false
            )
        }
    }
}
