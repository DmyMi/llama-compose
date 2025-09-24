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
package cloud.dmytrominochkin.ai.llamacompose.agent.client

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.serialization.serializeToolDescriptorsToJsonString
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import cloud.dmytrominochkin.ai.llamacompose.llama.ChatMessage
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaModel
import cloud.dmytrominochkin.ai.llamacompose.llama.Models
import cloud.dmytrominochkin.ai.llamacompose.llama.logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json


data object LlamaProvider : LLMProvider("llama-cpp", "Llama C++")

fun provideLlamaLLModel(): LLModel {
    return LLModel(
        provider = LlamaProvider,
        id = "3.2",
        capabilities = listOf(LLMCapability.Temperature, LLMCapability.Completion, LLMCapability.ToolChoice),
        // We're setting it from settings
        contextLength = 2048,
        maxOutputTokens = 512
    )
}

class LlamaLLMClient(private var llama: LlamaModel) : LLMClient {

    private val logger = logger()

    // List of blocked keywords for moderation demo. If empty, moderation always passes.
    private val blockedKeywords = listOf<String>() // e.g., listOf("bannedword1", "bannedword2")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

        override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Response> {
        if (!llama.isModelLoaded) {
            return listOf(
                Message.Assistant(
                    content = "Model is not loaded. Please load a model and try again.",
                    metaInfo = ResponseMetaInfo(Clock.System.now())
                )
            )
        }
        // TODO: this is a crutch, as Agent manages context and it needs some additional handling
        // TODO: on the model side to feed only new tokens, so that the context does not explode.
        // TODO: If context compression happens - it is difficult to manage
        llama.clearConversation()
        val modelType = llama.getLoadedModelType()
        val toolDescription = if (tools.isNotEmpty()) {
            serializeToolDescriptorsToJsonString(tools)
        } else {
            ""
        }
        val chatMessages = promptToChatMessages(prompt, modelType, toolDescription)

        val flow = this.llama.generateChat(chatMessages, maxTokens = model.maxOutputTokens?.toInt() ?: 512)
        val result = flow.toList().joinToString("")

        val toolCalls = parseToolCalls(result)
        return if (!toolCalls.isNullOrEmpty()) {
            toolCalls
        } else {
            listOf(Message.Assistant(content = result, metaInfo = ResponseMetaInfo(Clock.System.now())))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel
    ): Flow<String> = flow {
        if (!llama.isModelLoaded) {
            return@flow
        }
        llama.clearConversation()
        val modelType = llama.getLoadedModelType()
        val chatMessages = promptToChatMessages(prompt, modelType, "")
        emit(chatMessages)
    }
        .flatMapConcat { chatMessages ->
            llama.generateChat(
                chatMessages,
                maxTokens = model.maxOutputTokens?.toInt() ?: 512
            )
        }

    override suspend fun moderate(
        prompt: Prompt,
        model: LLModel
    ): ModerationResult {
        if (blockedKeywords.isEmpty()) return ModerationResult(isHarmful = false, categories = emptyMap())
        val text = prompt.toString().lowercase()
        val found = blockedKeywords.firstOrNull { it.lowercase() in text }
        return if (found != null) {
            ModerationResult(isHarmful = true, categories = emptyMap())
        } else {
            ModerationResult(isHarmful = false, categories = emptyMap())
        }
    }

    private fun promptToChatMessages(
        prompt: Prompt,
        modelType: Models,
        serializedToolDescription: String
    ): List<ChatMessage> =
        prompt.messages.map { msg ->
            when (msg) {
                is Message.System -> {
                    when (modelType) {
                        Models.Groq -> buildGroqSystemPrompt(msg.content, serializedToolDescription)
                        Models.Gemma -> buildGemmaSystemPrompt(msg.content, serializedToolDescription)
                        Models.Llama -> buildLlamaSystemPrompt(msg.content, serializedToolDescription)
                    }
                }

                is Message.User -> ChatMessage("user", msg.content)
                is Message.Assistant -> ChatMessage("assistant", msg.content)
                is Message.Tool.Result ->
                    when (modelType) {
                        Models.Groq -> ChatMessage(
                            "tool",
                            "<tool_response>\n${msg.content}\n<tool_response>"
                        )

                        Models.Gemma -> ChatMessage("user", msg.content)
                        Models.Llama -> ChatMessage("ipython", msg.content)
                    }

                is Message.Tool.Call ->
                    when (modelType) {
                        Models.Groq -> ChatMessage(
                            "assistant",
                            "<tool_call>\n${
                                json.encodeToString(
                                    GroqToolCall(
                                        msg.id,
                                        msg.tool,
                                        json.decodeFromString(msg.content)
                                    )
                                )
                            }\n</tool_call>"
                        )

                        Models.Gemma -> ChatMessage(
                            "assistant",
                            json.encodeToString(
                                RegularToolCall(
                                    msg.id ?: "",
                                    msg.tool,
                                    json.decodeFromString(msg.content)
                                )
                            )
                        )

                        Models.Llama -> ChatMessage(
                            "assistant",
                            "<|python_tag|>${
                                json.encodeToString(
                                    RegularToolCall(
                                        msg.id,
                                        msg.tool,
                                        json.decodeFromString(msg.content)
                                    )
                                )
                            }<|eom_id|>"
                        )
                    }
            }
        }

    private fun parseToolCalls(response: String): List<Message.Tool.Call>? {
        return try {
            val jsonPayload = extractToolCallJson(response).replace("\\", "")
            if (jsonPayload.startsWith("[") && jsonPayload.endsWith("]")) {
                val wire = json.decodeFromString<List<ToolCallWire>>(jsonPayload)
                wire.map(::buildMessageToolCall)
            }
            val wire = json.decodeFromString<ToolCallWire>(jsonPayload)
            listOf(buildMessageToolCall(wire))
        } catch (e: Exception) {
            logger.debug(e) { "Error parsing tool call response when it is not a valid json." }
            null
        }
    }

    private fun extractToolCallJson(response: String): String {
        val trimmed = response.trim()
        // Llama 3.2 parsing
        val cleaned = trimmed.replace("<|python_tag|>", "")

        // Llama 3 Groq parsing
        val groqRegex = Regex("""(?s)<tool_call>\s*(.*?)\s*</tool_call>""")
        val groqMatch = groqRegex.find(cleaned)
        if (groqMatch != null) {
            return groqMatch.groupValues[1]
        }

        // Gemma parsing, Gemma can easily get off track with extra tokens
        val mdRegex = Regex("""(?s)```(json|tool_code|tool_call)\s*(.*?)\s*```""")
        val mdMatch = mdRegex.find(cleaned)
        if (mdMatch != null) {
            return mdMatch.groupValues[2]
        }

        return cleaned
    }

    private fun buildMessageToolCall(wire: ToolCallWire): Message.Tool.Call {
        return Message.Tool.Call(
            id = wire.id,
            tool = wire.name,
            content = wire.args?.toString() ?: "{}",
            metaInfo = ResponseMetaInfo.create(Clock.System)
        )
    }
}
