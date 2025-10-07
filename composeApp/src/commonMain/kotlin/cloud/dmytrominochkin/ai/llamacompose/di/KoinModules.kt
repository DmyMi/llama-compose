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

import androidx.compose.runtime.Composable
import cloud.dmytrominochkin.ai.llamacompose.agent.AgentChatViewModel
import cloud.dmytrominochkin.ai.llamacompose.download.ModelDownloadViewModel
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaModel
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaModelCommon
import cloud.dmytrominochkin.ai.llamacompose.main.MainViewModel
import cloud.dmytrominochkin.ai.llamacompose.settings.SettingsRepository
import cloud.dmytrominochkin.ai.llamacompose.simple.SimpleChatViewModel
import cloud.dmytrominochkin.ai.llamacompose.state.StateViewModel
import org.koin.compose.KoinMultiplatformApplication
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.module

// Common module - platform context will be provided by platform-specific setup
@OptIn(KoinExperimentalAPI::class)
@Composable
fun KoinApp(content: @Composable () -> Unit) = KoinMultiplatformApplication(
    config = KoinConfiguration {
        modules(
            platformModule,
            module {
                // Single shared LlamaModel instance
                single<LlamaModel> { LlamaModelCommon() }

                // Main app state management
                viewModel { MainViewModel(get(), get()) }

                // New Flow Chat ViewModels
                viewModel { SimpleChatViewModel(get()) }
                viewModel { AgentChatViewModel(get()) }

                // Model download
                viewModel { ModelDownloadViewModel(get()) }

                viewModel { StateViewModel() }
            }
        )
    }
) {
    content()
}

// Platform-specific modules will be added in respective source sets
expect val platformModule: Module
