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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModelDownloadViewModel(
    private val repository: ModelRepository
) : ViewModel() {

    val models: StateFlow<List<ModelUiState>> = repository.models

    init {
        // Make sure we start with an up-to-date list
        viewModelScope.launch { repository.refresh() }
    }

    fun download(model: LlmModel) {
        viewModelScope.launch { repository.startDownload(model) }
    }

    fun cancel(model: LlmModel) {
        viewModelScope.launch { repository.cancelDownload(model) }
    }

    fun delete(model: LlmModel) {
        viewModelScope.launch { repository.deleteLocal(model) }
    }

    fun localPath(model: LlmModel): String? = repository.localPathFor(model)
}
