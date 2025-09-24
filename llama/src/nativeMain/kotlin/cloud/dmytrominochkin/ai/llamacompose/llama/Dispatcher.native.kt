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

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.newSingleThreadContext
import kotlin.coroutines.cancellation.CancellationException

internal actual fun llamaDispatcher(): LlamaDispatcher = LlamaDispatcherNative()

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
internal class LlamaDispatcherNative() : LlamaDispatcher {
    private val dispatcherImpl: CloseableCoroutineDispatcher = newSingleThreadContext("Llama-Native")
    override val dispatcher: CoroutineDispatcher = dispatcherImpl

    override fun close() {
        runCatching {
            dispatcherImpl.cancel(CancellationException("Closing LlamaDispatcher."))
            dispatcherImpl.close()
        }
    }
} 
