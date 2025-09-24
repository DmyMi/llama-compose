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

enum class ModelCategory {
    DESKTOP,
    MOBILE,
    COMPACT,
}

enum class ModelStatus {
    READY,
    DOWNLOADING,
    DOWNLOADED,
    ERROR,
}

data class LlmModel(
    val name: String,
    val filename: String,
    val sourceUrl: String,
    val sizeReadable: String? = "", // Optional nice-looking size string ( e.g. "0.6 GiB" )
    val description: String = "", // Markdown description of the model
    val quantization: String = "", // Quantization method (e.g., "Q4_K_M", "F16")
    val capabilities: String = "📝", // Emoji representing model capabilities (default: text-only)
    val category: ModelCategory,
)

data class ModelUiState(
    val model: LlmModel,
    val status: ModelStatus = ModelStatus.READY,
    val progress: Double = 0.0,
    val errorMessage: String? = null,
    val hasPartial: Boolean = false,
    val progressIndeterminate: Boolean = false, // For downloads with unknown total size
    val bytesPerSecond: Long? = null,
    val etaSeconds: Long? = null,
    val statusMessage: String? = null,
)

/** A single source of truth with a curated list of models we want to expose on every platform. */
object DefaultModels {
    val list = listOf(
        LlmModel(
            name = "Llama 3 Groq 8B Tool Use",
            filename = "Llama-3-Groq-8B-Tool-Use-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/bartowski/Llama-3-Groq-8B-Tool-Use-GGUF/resolve/main/Llama-3-Groq-8B-Tool-Use-Q4_K_M.gguf",
            sizeReadable = "4.9 GiB",
            description = """
                ## Llama 3 Groq 8B Tool Use

                This is a fine-tuned version of Llama 3 optimized for tool calling and function execution. It excels at understanding and using external tools and APIs.

                **Key Features:**
                - Advanced tool calling capabilities
                - Function execution and API integration
                - Optimized for practical applications
                - High performance on tool-related tasks
            """.trimIndent(),
            quantization = "Q4_K_M",
            capabilities = "📝 🔧 🖥️",
            category = ModelCategory.DESKTOP,
        ),
        LlmModel(
            name = "Gemma 3n E4B IT",
            filename = "gemma-3n-E4B-it-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/unsloth/gemma-3n-E4B-it-GGUF/resolve/main/gemma-3n-E4B-it-Q4_K_M.gguf?download=true",
            sizeReadable = "4.5 GiB",
            description = """
                ## Gemma 3n E4B Instruction Tuned

                A compact 4 billion parameter instruction-tuned model from Google's Gemma series. Offers excellent performance with reasonable resource usage.

                **Key Features:**
                - Efficient instruction following
                - Good balance of speed and quality
                - Suitable for various conversational tasks
                - Optimized for edge deployment
                - 4 billion parameters for solid capabilities
            """.trimIndent(),
            quantization = "Q4_K_M",
            capabilities = "📝 🔧 📱",
            category = ModelCategory.MOBILE,
        ),
        LlmModel(
            name = "Gemma 3n E2B IT",
            filename = "gemma-3n-E2B-it-Q5_K_M.gguf",
            sourceUrl = "https://huggingface.co/unsloth/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-E2B-it-Q5_K_M.gguf?download=true",
            sizeReadable = "3.3 GiB",
            description = """
                ## Gemma 3n E2B Instruction Tuned

                A lightweight 2 billion parameter instruction-tuned model from Google's Gemma series. Optimized for speed and efficiency with minimal resource requirements.

                **Key Features:**
                - Fast inference speed
                - Low memory footprint
                - Efficient instruction following
                - Great for mobile and edge devices
                - 2 billion parameters for good capabilities
            """.trimIndent(),
            quantization = "Q5_K_M",
            capabilities = "📝 🔧 📱",
            category = ModelCategory.MOBILE,
        ),
        LlmModel(
            name = "Llama 3.2 3B Instruct",
            filename = "Llama-3.2-3B-Instruct-Q5_K_M.gguf",
            sourceUrl = "https://huggingface.co/unsloth/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q5_K_M.gguf?download=true",
            sizeReadable = "2.3 GiB",
            description = """
                ## Llama 3.2 3B Instruct

                A compact yet powerful instruction-tuned model from the Llama 3.2 series. Perfect balance of performance and resource usage.

                **Key Features:**
                - Efficient instruction following
                - Good balance of speed and quality
                - Suitable for various conversational tasks
                - Optimized for edge deployment
            """.trimIndent(),
            quantization = "Q5_K_M",
            capabilities = "📝 🔧 🖥️",
            category = ModelCategory.DESKTOP,
        ),
        LlmModel(
            name = "Llama 3.2 1B Instruct",
            filename = "Llama-3.2-1B-Instruct-Q5_K_M.gguf",
            sourceUrl = "https://huggingface.co/unsloth/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q5_K_M.gguf?download=true",
            sizeReadable = "0.9 GiB",
            description = """
                ## Llama 3.2 1B Instruct

                The smallest model in the Llama 3.2 series, optimized for speed and minimal resource usage while maintaining good instruction following capabilities.

                **Key Features:**
                - Extremely fast inference
                - Low memory footprint
                - Great for mobile and edge devices
                - Good for simple conversational tasks
            """.trimIndent(),
            quantization = "Q5_K_M",
            capabilities = "📝 📟",
            category = ModelCategory.COMPACT,
        ),
        LlmModel(
            name = "Gemma 3 270m IT",
            filename = "gemma-3-270m-it-F16.gguf",
            sourceUrl = "https://huggingface.co/unsloth/gemma-3-270m-it-GGUF/resolve/main/gemma-3-270m-it-F16.gguf?download=true",
            sizeReadable = "0.5 GiB",
            description = """
                ## Gemma 3 270M Instruction Tuned

                A lightweight instruction-tuned model from Google's Gemma series. Excellent for basic conversational tasks with minimal resource requirements.

                **Key Features:**
                - Ultra-lightweight model
                - Fast inference speed
                - Good instruction following
                - Perfect for testing and development
            """.trimIndent(),
            quantization = "F16",
            capabilities = "📝 📟",
            category = ModelCategory.COMPACT,
        ),
    )
}
