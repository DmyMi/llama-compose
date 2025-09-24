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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal actual fun llamaDispatcher(): LlamaDispatcher = LlamaDispatcherJvm()

internal class LlamaDispatcherJvm() : LlamaDispatcher {
    private val executor = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llama-Jvm") { it.run() }
    }
    override val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    override fun close() {
        runCatching { executor.shutdown() }
    }
} 
