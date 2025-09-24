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

object ContactsLookupTool : Tool<ContactsLookupTool.Args, ContactsLookupTool.Result>() {
    @Serializable
    data class Args(val query: String) : ToolArgs

    @Serializable
    data class Result(val contacts: List<Contact>) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "contacts_lookup",
        description = "Find contacts by name/email/phone partial match.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "query",
                description = "Name, email, or phone substring.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val q = args.query.lowercase()
        val contacts = assistantMockFlow.value.contacts
        val list = contacts.filter { c ->
            c.name.lowercase().contains(q) ||
                c.emails.any { it.lowercase().contains(q) } ||
                c.phones.any { it.contains(q) }
        }
        return Result(list)
    }
}
