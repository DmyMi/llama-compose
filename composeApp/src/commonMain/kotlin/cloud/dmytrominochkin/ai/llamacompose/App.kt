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
package cloud.dmytrominochkin.ai.llamacompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cloud.dmytrominochkin.ai.llamacompose.agent.AgentChatScreenWrapper
import cloud.dmytrominochkin.ai.llamacompose.components.LifecycleAwareComposable
import cloud.dmytrominochkin.ai.llamacompose.main.MainScaffold
import cloud.dmytrominochkin.ai.llamacompose.main.MainViewModel
import cloud.dmytrominochkin.ai.llamacompose.onboard.OnboardingScreen
import cloud.dmytrominochkin.ai.llamacompose.proto.Theme
import cloud.dmytrominochkin.ai.llamacompose.settings.SettingsScreen
import cloud.dmytrominochkin.ai.llamacompose.simple.SimpleChatScreenWrapper
import cloud.dmytrominochkin.ai.llamacompose.state.StateScreen
import cloud.dmytrominochkin.ai.llamacompose.theme.AppThemeConfig
import cloud.dmytrominochkin.ai.llamacompose.theme.LlamaComposeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val mainViewModel: MainViewModel = koinViewModel()

    runBlocking {
        val settings = mainViewModel.config.first()
        val theme = settings.theme
        AppThemeConfig.userOverride.value = if (theme == Theme.THEME_UNSPECIFIED) Theme.THEME_AUTO else theme
    }
    changeLanguage("en")

    LlamaComposeTheme {
        val config by mainViewModel.config.collectAsState(null)

        LifecycleAwareComposable { _, event ->
            when (event) {
                Lifecycle.Event.ON_DESTROY -> mainViewModel.unloadModel()
                else -> { /* noop */
                }
            }
        }

        when (config?.is_on_boarding_completed) {
            null -> {
                // Config not loaded yet – show a minimal loading UI and avoid navigation
                Surface {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            else -> {
                val startDestination = if (config!!.is_on_boarding_completed) Routes.Main else Routes.Onboarding
                val rootNavController = rememberNavController()
                NavHost(
                    navController = rootNavController,
                    startDestination = startDestination
                ) {
                    composable<Routes.Onboarding> {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                mainViewModel.saveOnBoardingState(true)
                                rootNavController.navigate(Routes.Main) {
                                    popUpTo(Routes.Onboarding) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable<Routes.Main> {
                        val navController = rememberNavController()
                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        MainScaffold(
                            mainViewModel = mainViewModel,
                            navController = navController,
                            currentRoute = currentBackStackEntry,
                            navigationBar = {
                                if (currentBackStackEntry?.destination?.hasRoute<Routes.MainRoutes.Chat>() == true ||
                                    currentBackStackEntry?.destination?.hasRoute<Routes.MainRoutes.Agent>() == true) {
                                    BottomNavigation(
                                        currentRoute = currentBackStackEntry,
                                        onRouteSelected = { route: Routes ->
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            },
                            content = {
                                NavHost(
                                    navController = navController,
                                    startDestination = Routes.MainRoutes.Chat
                                ) {
                                    composable<Routes.MainRoutes.Chat> {
                                        SimpleChatScreenWrapper(mainViewModel = mainViewModel)
                                    }
                                    composable<Routes.MainRoutes.Agent> {
                                        AgentChatScreenWrapper(
                                            mainViewModel = mainViewModel,
                                            showAppState = {
                                                navController.navigate(Routes.MainRoutes.State)
                                            })
                                    }
                                    composable<Routes.MainRoutes.Settings> {
                                        SettingsScreen(
                                            mainViewModel = mainViewModel,
                                            onDone = { navController.popBackStack() }
                                        )
                                    }
                                    composable<Routes.MainRoutes.State> {
                                        StateScreen(
                                            onDone = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
