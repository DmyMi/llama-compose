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
package cloud.dmytrominochkin.ai.llamacompose.llama

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext


class LlamaModelCommon(
    private var modelParams: LlamaParams = LlamaParams()
) : LlamaModel {

    private val logger = logger()

    private val llamaDispatcher: LlamaDispatcher = llamaDispatcher()

    // Create a single platform instance that will be reused for multiple models
    private val platformApi: PlatformLlama = initializePlatformLlama()

    override suspend fun loadModel(modelPath: String, samplingParams: LlamaSamplingParams): Boolean =
        withContext(llamaDispatcher.dispatcher) {
            try {
                platformApi.loadModel(modelPath, modelParams, samplingParams)
            } catch (e: Exception) {
                logger.error(e) { "Error loading model" }
                false
            }
        }

    override suspend fun generateText(prompt: String, maxTokens: Int): Flow<String> {
        if (!platformApi.isModelLoaded) error("Model not loaded. Call loadModel() first.")
        return platformApi.generateText(prompt, maxTokens).flowOn(llamaDispatcher.dispatcher)
    }

    override suspend fun generateChat(messages: List<ChatMessage>, maxTokens: Int): Flow<String> {
        if (!platformApi.isModelLoaded) error("Model not loaded. Call loadModel() first.")
        val fullPrompt = platformApi.applyChatTemplate(messages, addAssistantPrompt = true)
        return generateText(fullPrompt, maxTokens)
    }

    /**
     * Starts a new, clean chat session, clearing all internal memory in the native library.
     */
    override fun clearConversation() {
        if (platformApi.isModelLoaded) {
            platformApi.clearConversationState()
        }
    }

    /**
     * Request cooperative cancellation of any in-flight generation.
     */
    override fun requestCancel() {
        platformApi.requestCancel()
    }

    /**
     * Generates the next turn in the conversation based on the user's message.
     * The native library manages conversation history internally via KV cache.
     * Only the new user message needs to be passed to the lower level library.
     */
    override suspend fun continueConversation(userMessage: String, maxTokens: Int): Flow<String> {
        if (!platformApi.isModelLoaded) error("Model not loaded. Call loadModel() first.")

        return generateChat(listOf(ChatMessage(role = "user", content = userMessage)), maxTokens)
    }

    override suspend fun unloadModel() {
        withContext(llamaDispatcher.dispatcher) { platformApi.unloadModel() }
        // Do not cancel the scope here, allow reloading into the same instance.
    }

    /**
     * Free the backend resources. Should be called once on application termination.
     * This is separate from unloadModel() which only frees model-specific resources.
     */
    override fun freeBackend() {
        platformApi.freeBackend()
        llamaDispatcher.close()
    }

    override val isModelLoaded: Boolean
        get() = platformApi.isModelLoaded

    /**
     * Update the sampling parameters dynamically without reloading the model.
     */
    override suspend fun updateSamplingParams(samplingParams: LlamaSamplingParams): Boolean =
        withContext(llamaDispatcher.dispatcher) {
            platformApi.updateSamplingParams(samplingParams)
        }

    override suspend fun getModelMetaValue(key: String): String? = withContext(llamaDispatcher.dispatcher) {
        platformApi.getModelMetaValue(key)
    }

    /**
     * Returns KV-cache usage percentage for the default sequence (seqId = 0).
     */
    override suspend fun getContextUsagePercent(seqId: Int): Float = withContext(llamaDispatcher.dispatcher) {
        platformApi.getContextUsagePercent(seqId)
    }

    override suspend fun getLoadedModelType(): Models = withContext(llamaDispatcher.dispatcher) {
        platformApi.getModelMetaValue("general.name")?.let { modelName ->
            if (modelName.contains("groq", true)) Models.Groq
            else if (modelName.contains("gemma", true)) Models.Gemma
            else Models.Llama
        } ?: Models.Llama // This should not happen for a loaded model
    }

    override fun setModelParams(params: LlamaParams) {
        modelParams = params
    }

    override fun getModelParams(): LlamaParams = modelParams

    override val supportsGpuOffloading: Boolean
        get() = platformApi.supportsGpuOffloading
}
