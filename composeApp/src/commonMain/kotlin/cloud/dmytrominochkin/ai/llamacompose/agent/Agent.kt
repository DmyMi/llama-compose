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
package cloud.dmytrominochkin.ai.llamacompose.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.model.PromptExecutor
import cloud.dmytrominochkin.ai.llamacompose.agent.client.provideLlamaLLModel
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.AddDatetimeTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.CalendarCreateEventTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.CalendarFindAvailabilityTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.CalendarListEventsTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.ContactsLookupTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.ExitTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.NoteCreateTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.NoteReadTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.NotesSearchTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.PlacesSearchTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.TaskCompleteTool
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.TasksListTool
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.error_handling
import cloud.dmytrominochkin.ai.llamacompose.resources.error_tool_call_failed
import cloud.dmytrominochkin.ai.llamacompose.resources.status_analyzing
import cloud.dmytrominochkin.ai.llamacompose.resources.status_processing_results
import cloud.dmytrominochkin.ai.llamacompose.resources.status_tool_calling
import org.jetbrains.compose.resources.getString

fun createAgent(
    promptExecutor: () -> PromptExecutor,
    onToolCall: suspend (String) -> Unit,
    onToolCallFailure: suspend (String) -> Unit,
    onToolCallResult: suspend (String) -> Unit,
    onBeforeLLMCall: suspend (String) -> Unit,
    onAfterLLMCall: suspend () -> Unit,
    onAgentRunError: suspend (String) -> Unit,
    onAssistantMessage: suspend (String) -> String,
): AIAgent<String, String> {

    val toolRegistry = ToolRegistry {
        tool(AddDatetimeTool)
        tool(NotesSearchTool)
        tool(NoteReadTool)
        tool(NoteCreateTool)
        tool(CalendarListEventsTool)
        tool(CalendarFindAvailabilityTool)
        tool(CalendarCreateEventTool)
        tool(TasksListTool)
        tool(TaskCompleteTool)
        tool(ContactsLookupTool)
        tool(PlacesSearchTool)
        tool(ExitTool)
    }

    val strategy = strategy("Personal Assistant") {
        val nodeCallLLM by nodeLLMRequestMultiple()
        val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }
        val nodeExecuteTool by nodeExecuteMultipleTools(parallelTools = false)
        val nodeSendToolResult by nodeLLMSendMultipleToolResults()
        val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

        edge(nodeStart forwardTo nodeCallLLM)

        edge(
            nodeCallLLM forwardTo nodeExecuteTool
                onMultipleToolCalls { true }
        )

        edge(
            nodeCallLLM forwardTo nodeAssistantMessage
                transformed { it.first() }
                onAssistantMessage { true }
        )

        edge(nodeAssistantMessage forwardTo nodeCallLLM)

        // Finish if ExitTool is invoked
        edge(
            nodeExecuteTool forwardTo nodeFinish
                onCondition { it.singleOrNull()?.tool == ExitTool.name }
                transformed { it.single().result!!.toStringDefault() }
        )

        edge(
            nodeExecuteTool forwardTo nodeCompressHistory
                onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
        )

        edge(nodeCompressHistory forwardTo nodeSendToolResult)

        edge(
            nodeExecuteTool forwardTo nodeSendToolResult
                onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
        )

        edge(
            nodeSendToolResult forwardTo nodeExecuteTool
                onMultipleToolCalls { true }
        )

        edge(
            nodeSendToolResult forwardTo nodeAssistantMessage
                transformed { it.first() }
                onAssistantMessage { true }
        )
    }

    return AIAgent(
        executor = promptExecutor(),
        llmModel = provideLlamaLLModel(),
        strategy = strategy,
        toolRegistry = toolRegistry,
        systemPrompt = """
You are Personal Assistant named Llema.

Primary capabilities:
- Manage and consult notes, calendar, tasks, and contacts using functions.
- When dates/times are involved, ALWAYS use ${AddDatetimeTool.descriptor.name} functions for calculations, do not try to guess.

Workflow rules:
1) Before acting, identify missing details (date/time, attendees, location, constraints). If ambiguous, ask ONE clear clarifying question.
2) Prefer the smallest, most relevant function. Execute tools sequentially and wait for results before the next step.
3) Before creating events or making changes, confirm with the user by summarizing intent.
4) After using functions, summarize what you found/changed and propose next steps.
5) When done (or upon user request to finish or exit the conversation), call ${ExitTool.descriptor.name} properly.

Style:
- Be brief, structured, and helpful. Use markdown and short lists.
- When you ask a clarifying question, keep it to a single question.
""".trimIndent(),
        maxIterations = 50
    ) {
        handleEvents {
            onToolCall { eventContext ->
                val newEvent = getString(Res.string.status_tool_calling, eventContext.tool.name, eventContext.toolArgs)
                onToolCall(newEvent)
            }
            onToolCallFailure { eventContext ->
                val newEvent = getString(Res.string.error_tool_call_failed, eventContext.tool.name)
                onToolCallFailure(newEvent)
            }
            onToolCallResult { eventContext ->
                val newEvent = getString(Res.string.status_processing_results, eventContext.tool.name)
                onToolCallResult(newEvent)
            }
            onBeforeLLMCall { _ ->
                onBeforeLLMCall(getString(Res.string.status_analyzing))
            }
            onAfterLLMCall { _ ->
                onAfterLLMCall()
            }
            onAgentRunError { _ ->
                // TODO: make it better :)
                val newEvent = getString(Res.string.error_handling)
                onAgentRunError(newEvent)
            }
        }
    }
}
