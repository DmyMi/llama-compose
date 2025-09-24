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
package cloud.dmytrominochkin.ai.llamacompose.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import cloud.dmytrominochkin.ai.llamacompose.Routes
import cloud.dmytrominochkin.ai.llamacompose.download.ModelDownloadSheet
import cloud.dmytrominochkin.ai.llamacompose.download.ModelDownloadViewModel
import cloud.dmytrominochkin.ai.llamacompose.proto.Theme
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_back
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_select_model
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_settings
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_unload_model
import cloud.dmytrominochkin.ai.llamacompose.resources.model_ready
import cloud.dmytrominochkin.ai.llamacompose.resources.theme_auto
import cloud.dmytrominochkin.ai.llamacompose.resources.theme_dark
import cloud.dmytrominochkin.ai.llamacompose.resources.theme_light
import cloud.dmytrominochkin.ai.llamacompose.resources.title_agent_chat
import cloud.dmytrominochkin.ai.llamacompose.resources.title_agent_state
import cloud.dmytrominochkin.ai.llamacompose.resources.title_chat
import cloud.dmytrominochkin.ai.llamacompose.resources.title_llamacompose
import cloud.dmytrominochkin.ai.llamacompose.resources.title_settings
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import cloud.dmytrominochkin.ai.llamacompose.theme.AppThemeConfig
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    mainViewModel: MainViewModel,
    navController: NavController,
    currentRoute: NavBackStackEntry?,
    navigationBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by mainViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val modelDownloadViewModel: ModelDownloadViewModel = koinViewModel()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            currentRoute?.destination?.hasRoute<Routes.MainRoutes.Settings>() == true -> stringResource(
                                Res.string.title_settings
                            )

                            currentRoute?.destination?.hasRoute<Routes.MainRoutes.Agent>() == true -> stringResource(Res.string.title_agent_chat)
                            currentRoute?.destination?.hasRoute<Routes.MainRoutes.Chat>() == true -> stringResource(Res.string.title_chat)
                            currentRoute?.destination?.hasRoute<Routes.MainRoutes.State>() == true -> stringResource(Res.string.title_agent_state)
                            else -> stringResource(Res.string.title_llamacompose)
                        }
                    )
                },
                navigationIcon = {
                    if (
                        currentRoute?.destination?.hasRoute<Routes.MainRoutes.Settings>() == true ||
                        currentRoute?.destination?.hasRoute<Routes.MainRoutes.State>() == true
                        ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back)
                            )
                        }
                    }
                },
                actions = {
                    if (
                        currentRoute?.destination?.hasRoute<Routes.MainRoutes.Chat>() == true ||
                        currentRoute?.destination?.hasRoute<Routes.MainRoutes.Agent>() == true
                    ) {
                        var showThemeMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(onClick = { showThemeMenu = !showThemeMenu }) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = "Theme"
                                )
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                val selectedTheme = AppThemeConfig.userOverride.value

                                listOf(
                                    Theme.THEME_AUTO to stringResource(Res.string.theme_auto),
                                    Theme.THEME_LIGHT to stringResource(Res.string.theme_light),
                                    Theme.THEME_DARK to stringResource(Res.string.theme_dark)
                                ).forEach { (theme, name) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = selectedTheme == theme,
                                                    onClick = null
                                                )
                                                Spacer(modifier = Modifier.width(AppDimension.spacingS))
                                                Text(name)
                                            }
                                        },
                                        onClick = {
                                            mainViewModel.setTheme(theme)
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { navController.navigate(Routes.MainRoutes.Settings) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(Res.string.cd_settings)
                            )
                        }

                        IconButton(onClick = { mainViewModel.toggleModelDownloadSheet() }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(Res.string.cd_select_model)
                            )
                        }

                        if (uiState.isModelLoaded) {
                            IconButton(onClick = { mainViewModel.unloadModel() }) {
                                Icon(
                                    imageVector = Icons.Default.Eject,
                                    contentDescription = stringResource(Res.string.cd_unload_model)
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = navigationBar,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        MainContent(
            paddingValues = paddingValues,
            content = content
        )
    }

    LaunchedEffect(uiState.isModelLoaded, uiState.isModelLoading, uiState.currentModelName) {
        if (uiState.isModelLoaded && !uiState.isModelLoading && uiState.currentModelName.isNotBlank()) {
            snackbarHostState.showSnackbar(getString(Res.string.model_ready, uiState.currentModelName))
        }
    }

    LaunchedEffect(uiState.modelLoadErrorMessage) {
        val message = uiState.modelLoadErrorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            mainViewModel.clearModelLoadError()
        }
    }

    if (uiState.showModelDownloadSheet) {
        ModelDownloadSheet(
            mainViewModel = mainViewModel,
            modelDownloadViewModel = modelDownloadViewModel,
            snackbarHostState = snackbarHostState,
            onDismiss = { mainViewModel.hideModelDownloadSheet() }
        )
    }
}

@Composable
private fun MainContent(
    paddingValues: PaddingValues,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        content()
    }
}
