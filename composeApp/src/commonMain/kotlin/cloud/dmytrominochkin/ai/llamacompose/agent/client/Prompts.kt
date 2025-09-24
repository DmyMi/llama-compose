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
package cloud.dmytrominochkin.ai.llamacompose.agent.client

import cloud.dmytrominochkin.ai.llamacompose.llama.ChatMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

private val currentDate
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        .format(LocalDateTime.Format {
            dayOfMonth()
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
        })

fun buildGroqSystemPrompt(systemMessage: String?, toolsDescription: String): ChatMessage {
    val toolMessage = if (toolsDescription.isEmpty()) {
        toolsDescription
    } else {
        """

You are provided with function signatures within <tools></tools> XML tags. You may call one or more functions to assist with the user query.
Don't make assumptions about what values to plug into functions.
For each function call return a JSON object with function name and arguments within <tool_call></tool_call> XML tags as follows:
<tool_call>
{"name": <function-name>, "arguments": <args-dict>}
</tool_call>

Here are the available tools:
<tools>
$toolsDescription
</tools>
"""
    }

    return ChatMessage(
        "system",
        """
Cutting Knowledge Date: March 2023
Today Date: $currentDate
${systemMessage ?: ""}
$toolMessage
""".trimIndent()
    )
}

fun buildGemmaSystemPrompt(systemMessage: String?, toolsDescription: String): ChatMessage {
    val toolMessage = if (toolsDescription.isEmpty()) {
        toolsDescription
    } else {
        """
You have access to functions. If you decide to invoke any of the function(s), you MUST put it in the format of
{"name": function name, "parameters": dictionary of argument name and its value}

Do NOT include commentary NOR Markdown triple-backtick code blocks if you call a function. Do not use variables.

$toolsDescription

"""
    }
    return ChatMessage(
        "system",
        """
Cutting Knowledge Date: August 2024
Today Date: $currentDate
$toolMessage
${systemMessage ?: ""}
""".trimIndent())
}

fun buildLlamaSystemPrompt(systemMessage: String?, toolsDescription: String): ChatMessage {
    val toolMessage = if (toolsDescription.isEmpty()) {
        toolsDescription
    } else {
        """
Given the following functions, please respond with a JSON for a function call with its proper arguments that best answers the given prompt.

Respond in the format {"name": function name, "parameters": dictionary of argument name and its value}.
Do NOT include commentary NOR Markdown triple-backtick code blocks if you call a function. Do not use variables.

$toolsDescription

"""
    }
    return ChatMessage(
        "system",
        """
${if (toolMessage.isNotEmpty()) "Environment: ipython" else ""}
Cutting Knowledge Date: December 2023
Today Date: $currentDate
$toolMessage
${systemMessage ?: ""}
""".trimIndent()
    )
}
