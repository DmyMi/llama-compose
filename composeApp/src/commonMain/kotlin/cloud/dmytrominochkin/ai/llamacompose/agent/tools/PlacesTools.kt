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
package cloud.dmytrominochkin.ai.llamacompose.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

object PlacesSearchTool : Tool<PlacesSearchTool.Args, PlacesSearchTool.Result>() {
    @Serializable
    data class Args(val query: String, val near: String) : ToolArgs

    @Serializable
    data class Result(val places: List<Place>) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "places_search",
        description = "Find places near a given location.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "query",
                description = "Search query, e.g., 'sushi', 'coffee'.",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "near",
                description = "Reference location, free-form.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val candidates = assistantMockFlow.value.places
        val filtered = candidates.filter { p ->
            p.name.contains(args.query, ignoreCase = true) || args.query.isBlank()
        }
        return Result(filtered)
    }
}


