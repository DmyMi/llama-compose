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
package cloud.dmytrominochkin.ai.llamacompose.di

import cloud.dmytrominochkin.ai.llamacompose.PlatformContext
import cloud.dmytrominochkin.ai.llamacompose.download.KtorModelRepository
import cloud.dmytrominochkin.ai.llamacompose.download.ModelRepository
import cloud.dmytrominochkin.ai.llamacompose.settings.getDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDataStore(PlatformContext.INSTANCE) }
    single<ModelRepository> { KtorModelRepository(PlatformContext.INSTANCE) }
}
