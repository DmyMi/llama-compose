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
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import cloud.dmytrominochkin.ai.llamacompose.PlatformContext
import cloud.dmytrominochkin.ai.llamacompose.getUserDataDir
import cloud.dmytrominochkin.ai.llamacompose.proto.LlamaConfig
import okio.FileSystem
import okio.SYSTEM

fun getDataStore(context: PlatformContext): DataStore<LlamaConfig> =
    DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = LlamaConfigSerializer
        ) {
            getUserDataDir(context, "LlamaCompose") / "llamacompose.preferences_pb"
        },
        corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { getDefaultLlamaConfig() })
    )
