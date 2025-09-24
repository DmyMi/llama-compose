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

object CalendarListEventsTool : Tool<CalendarListEventsTool.Empty, CalendarListEventsTool.Result>() {
    @Serializable
    data object Empty : ToolArgs

    @Serializable
    data class Result(val events: List<CalendarEvent>) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Empty.serializer()

    override val descriptor = ToolDescriptor(
        name = "calendar_list_events",
        description = "List all scheduled events from calendar.",
    )

    override suspend fun execute(args: Empty): Result {
        val events = assistantMockFlow.value.calendarEvents
        return Result(events)
    }
}

object CalendarFindAvailabilityTool : Tool<CalendarFindAvailabilityTool.Args, CalendarFindAvailabilityTool.Result>() {
    @Serializable
    data class Args(
        val start: String,
        val end: String,
        val attendees: List<String>
    ) : ToolArgs

    @Serializable
    data class TimeRange(val start: String, val end: String)

    @Serializable
    data class Result(val free: List<TimeRange>) : ToolResult {
        override fun toStringDefault(): String = if (free.isEmpty()) "No free slots" else free.joinToString("\n") { "${it.start} - ${it.end}" }
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "calendar_find_availability",
        description = "Find free time slots between start and end for the given attendees.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "start",
                description = "Start datetime (ISO).",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "end",
                description = "End datetime (ISO).",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "attendees",
                description = "List of attendees for the event.",
                type = ToolParameterType.List(ToolParameterType.String)
            ),
        )
    )

    override suspend fun execute(args: Args): Result {
        // Very simple mock: return two slots not overlapping known events
        val free = buildList {
            add(TimeRange(start = "10:00:00", end = "11:00:00"))
            add(TimeRange(start = "12:00:00", end = "16:00:00"))
        }
        return Result(free)
    }
}

object CalendarCreateEventTool : Tool<CalendarCreateEventTool.Args, CalendarCreateEventTool.Result>() {
    @Serializable
    data class Args(
        val title: String,
        val start: String,
        val end: String,
        val attendees: List<String>,
        val location: String? = null
    ) : ToolArgs

    @Serializable
    data class Result(val eventId: String) : ToolResult {
        override fun toStringDefault(): String = "Created event $eventId"
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "calendar_create_event",
        description = "Create a calendar event with title, time range, attendees, and optional location.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "title",
                description = "Event title.",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "start",
                description = "Start datetime (ISO).",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "end",
                description = "End datetime (ISO).",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "attendees",
                description = "List of attendees for the event.",
                type = ToolParameterType.List(ToolParameterType.String)
            ),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                name = "location",
                description = "Event location.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val id = createEvent(
            title = args.title,
            start = args.start,
            end = args.end,
            attendees = args.attendees,
            location = args.location
        )
        return Result(eventId = id)
    }
}
