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

import androidx.datastore.core.DataStore
import cloud.dmytrominochkin.ai.llamacompose.proto.LlamaConfig
import kotlinx.coroutines.flow.Flow

/**
 * A small repository around Multiplatform Settings to persist and observe llama params.
 * Uses JSON serialization to store the full struct under a single key.
 */
class SettingsRepository(private val datastore: DataStore<LlamaConfig>) {

    val configFlow: Flow<LlamaConfig> = datastore.data

    suspend fun update(transform: (LlamaConfig) -> LlamaConfig) {
        datastore.updateData { transform(it) }
    }
}


