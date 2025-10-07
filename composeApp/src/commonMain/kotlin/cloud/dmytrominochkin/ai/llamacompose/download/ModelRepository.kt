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

import kotlinx.coroutines.flow.StateFlow

interface ModelRepository {
    /** A stream with a complete list of models and their current states. */
    val models: StateFlow<List<ModelUiState>>

    /** Forces a re-scan of the local storage (e.g. after application start). */
    suspend fun refresh()

    /** Begins downloading [model]. Ignored when the model is already downloaded. */
    suspend fun startDownload(model: LlmModel)

    /** Cancels an ongoing download (if any). */
    suspend fun cancelDownload(model: LlmModel)

    /** Removes the local copy of the model, if present. */
    suspend fun deleteLocal(model: LlmModel)

    /** Returns absolute path to the *local* file when present, `null` otherwise. */
    fun localPathFor(model: LlmModel): String?
}
