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

object TasksListTool : Tool<TasksListTool.Args, TasksListTool.Result>() {
    @Serializable
    data class Args(val status: String = "open", val tag: String? = null) : ToolArgs

    @Serializable
    data class Result(val tasks: List<Task>) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "tasks_list",
        description = "List tasks filtered by status and optional tag.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "status",
                description = "open | done | all",
                type = ToolParameterType.String
            )
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                name = "tag",
                description = "tag to filter",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val statusFilter = args.status.lowercase()
        val tagFilter = args.tag?.lowercase()
        val filtered = assistantMockFlow.value.tasks.filter { t ->
            val statusOk = when (statusFilter) {
                "open" -> t.status == TaskStatus.OPEN
                "done" -> t.status == TaskStatus.DONE
                else -> true
            }
            val tagOk = tagFilter?.let { tf -> t.tags.any { it.lowercase() == tf } } ?: true
            statusOk && tagOk
        }
        return Result(filtered)
    }
}

object TaskCompleteTool : Tool<TaskCompleteTool.Args, TaskCompleteTool.Result>() {
    @Serializable
    data class Args(val taskId: String) : ToolArgs

    @Serializable
    data class Result(val ok: Boolean) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "task_complete",
        description = "Mark a task as completed by id.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "taskId",
                description = "The id of the task to complete.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val ok = completeTask(args.taskId)
        return Result(ok)
    }
}
