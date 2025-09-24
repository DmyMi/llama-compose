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
package cloud.dmytrominochkin.ai.llamacompose.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import cloud.dmytrominochkin.ai.llamacompose.proto.GenerationConfig
import cloud.dmytrominochkin.ai.llamacompose.proto.LlamaConfig
import cloud.dmytrominochkin.ai.llamacompose.proto.ModelConfig
import cloud.dmytrominochkin.ai.llamacompose.proto.Theme
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

object LlamaConfigSerializer : OkioSerializer<LlamaConfig> {
    override val defaultValue: LlamaConfig
        get() = getDefaultLlamaConfig()

    override suspend fun readFrom(source: BufferedSource): LlamaConfig =
        try {
            LlamaConfig.ADAPTER.decode(source)
        } catch (e: IOException) {
            throw CorruptionException("Cannot read proto.", e)
        } catch (e: IllegalStateException) {
            throw CorruptionException("Cannot deserialize proto.", e)
        }

    override suspend fun writeTo(t: LlamaConfig, sink: BufferedSink) = t.encode(sink)
}

fun getDefaultGenerationConfig(): GenerationConfig = GenerationConfig(
    temperature = 0.6f,
    top_k = 63,
    top_p = 0.9f,
    repeat_penalty = 1.0f,
    seed = -1,
    greedy = false,
    min_p = 0.0f
)

fun getDefaultLlamaConfig(): LlamaConfig = LlamaConfig(
    theme = Theme.THEME_UNSPECIFIED,
    is_on_boarding_completed = false,
    model = ModelConfig(
        nctx = 4096,
        use_gpu = false
    ),
    generation = getDefaultGenerationConfig()
)
