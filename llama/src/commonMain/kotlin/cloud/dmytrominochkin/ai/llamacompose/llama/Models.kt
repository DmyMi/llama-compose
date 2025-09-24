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

import kotlinx.serialization.Serializable

data class LlamaParams(
    val nThreads: Int = getProcessorCount(),
    val nPredict: Int = 256,
    val nCtx: Int = 4096,
    val useGpu: Boolean = false,
)

data class LlamaSamplingParams(
    val temperature: Float = 0.6f, // Temperature for sampling
    val topK: Int = 63, // Top-K sampling
    val topP: Float = 0.9f, // Top-P sampling
    val minP: Float = 0.0f, // Minimum P sampling (0 disables)
    // TODO: Implement penalties UI
    val repeatPenalty: Float = 1.0f, // Discourage repetition
    val frequencyPenalty: Float = 0.0f, // Reduce frequent token usage
    val presencePenalty: Float = 0.0f, // Encourage topic diversity
    val seed: Int = -1, // Random seed for reproducibility
    val greedy: Boolean = false, // Greedy sampling (no sampling)
    // TODO: Implement penalties UI
    val penalise: Boolean = false // Apply penalties
)

@Serializable
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

enum class Models {
    Groq, Gemma, Llama
}
