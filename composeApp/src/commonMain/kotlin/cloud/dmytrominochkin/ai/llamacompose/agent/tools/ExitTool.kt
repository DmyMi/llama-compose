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

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.Serializable

object ExitTool : SimpleTool<ExitTool.Args>() {
    @Serializable
    data class Args(val finalizer: String = "") : ToolArgs

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "__exit__",
        description = "Exit the agent session with the specified result. Call this tool to finish the conversation with the user.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "finalizer",
                description = "The result of the agent session. Default is empty, if there's no particular result.",
                type = ToolParameterType.String,
            )
        )
    )

    override suspend fun doExecute(args: Args): String = args.finalizer
}
