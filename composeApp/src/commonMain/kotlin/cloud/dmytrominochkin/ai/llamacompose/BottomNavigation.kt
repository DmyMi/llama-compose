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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_agent
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_chat
import cloud.dmytrominochkin.ai.llamacompose.resources.nav_agent
import cloud.dmytrominochkin.ai.llamacompose.resources.nav_chat
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavigation(
    currentRoute: NavBackStackEntry?,
    onRouteSelected: (Routes) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(Res.string.cd_chat)) },
            label = { Text(stringResource(Res.string.nav_chat)) },
            selected = currentRoute?.destination?.hasRoute<Routes.MainRoutes.Chat>() == true,
            onClick = { onRouteSelected(Routes.MainRoutes.Chat) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.SmartToy, contentDescription = stringResource(Res.string.cd_agent)) },
            label = { Text(stringResource(Res.string.nav_agent)) },
            selected = currentRoute?.destination?.hasRoute<Routes.MainRoutes.Agent>() == true,
            onClick = { onRouteSelected(Routes.MainRoutes.Agent) }
        )
    }
}
