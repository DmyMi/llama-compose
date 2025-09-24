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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

/**
 * In-memory demo store with mock data for the personal assistant tools.
 */
val assistantMockFlow = MutableStateFlow(AssistantMockStore())

fun createNote(title: String, content: String, tags: List<String>): String {
    val id = "n-${assistantMockFlow.value.nextNoteId}"
    assistantMockFlow.update {
        it.copy(
            nextNoteId = it.nextNoteId + 1,
            notes = it.notes + Note(
                id = id,
                title = title,
                content = content,
                tags = tags,
                updatedAt = Clock.System.now().toString()
            )
        )
    }
    return id
}

fun createEvent(title: String, start: String, end: String, attendees: List<String>, location: String?): String {
    val id = "e-${assistantMockFlow.value.nextEventId}"
    assistantMockFlow.update {
        it.copy(
            nextEventId = it.nextEventId + 1,
            calendarEvents = it.calendarEvents + CalendarEvent(
                id = id,
                title = title,
                start = start,
                end = end,
                attendees = attendees,
                location = location
            )
        )
    }
    return id
}

fun completeTask(taskId: String): Boolean {
    val tsk = assistantMockFlow.value.tasks
    val idx = tsk.indexOfFirst { it.id == taskId }
    if (idx < 0) return false
    val t = tsk[idx]
    if (t.status == TaskStatus.DONE) return true
    assistantMockFlow.update {
        it.copy(
            tasks = it.tasks.mapIndexed { i, t1 -> if (i == idx) t1.copy(status = TaskStatus.DONE) else t1 },
        )
    }
    return true
}

data class AssistantMockStore(
    private val currentTimestamp: Instant = Clock.System.now(),
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    val nextNoteId: Int = 4,
    val nextEventId: Int = 3,
    val notes: List<Note> = mutableListOf(
        Note(
            id = "n-1",
            title = "Team lunch ideas",
            content = "Ben prefers sushi. Office near 123 Oak St.",
            tags = listOf("food", "team"),
            updatedAt = "2025-09-01T09:00:00+02:00"
        ),
        Note(
            id = "n-2",
            title = "Q4 planning",
            content = "Focus on onboarding and reliability. Target date: Oct 15.",
            tags = listOf("work", "planning"),
            updatedAt = "2025-09-10T14:20:00+02:00"
        ),
        Note(
            id = "n-3",
            title = "Personal errands",
            content = "Renew car registration; buy Labubu as birthday gift for Doug Patron.",
            tags = listOf("personal"),
            updatedAt = "2025-09-11T18:05:00+02:00"
        )
    ),
    val contacts: List<Contact> = mutableListOf(
        Contact(
            id = "c-1",
            name = "Ben Dover",
            birthday = getBirthday(currentTimestamp, timeZone, 8, 14),
            emails = listOf("ben.dover@example.com"),
            phones = listOf("+380-66-555-0101")
        ),
        Contact(
            id = "c-2",
            name = "Doug Patron",
            birthday = getBirthday(currentTimestamp, timeZone, 0, 7),
            emails = listOf("doug.patron@example.com"),
            phones = listOf("+380-66-555-0151")
        )
    ),
    val calendarEvents: List<CalendarEvent> = mutableListOf(
        CalendarEvent(
            id = "e-1",
            title = "Standup",
            start = LocalDateTime(
                (currentTimestamp + 1.days).toLocalDateTime(timeZone).date,
                LocalTime(9, 30, 0)
            ).toString(),
            end = LocalDateTime(
                (currentTimestamp + 1.days).toLocalDateTime(timeZone).date,
                LocalTime(10, 0, 0)
            ).toString(),
            attendees = listOf("you@example.com")
        ),
        CalendarEvent(
            id = "e-2",
            title = "Project sync",
            start = LocalDateTime(
                (currentTimestamp + 1.days).toLocalDateTime(timeZone).date,
                LocalTime(11, 0, 0)
            ).toString(),
            end = LocalDateTime(
                (currentTimestamp + 1.days).toLocalDateTime(timeZone).date,
                LocalTime(12, 0, 0)
            ).toString(),
            attendees = listOf("you@example.com", "doug.patron@example.com")
        )
    ),
    val tasks: List<Task> = mutableListOf(
        Task(
            id = "t-1",
            title = "Prepare Q4 outline",
            status = TaskStatus.OPEN,
            due = (currentTimestamp + 3.days).toLocalDateTime(timeZone).date.toString(),
            tags = listOf("work")
        ),
        Task(
            id = "t-2",
            title = "Buy gift for Doug",
            status = TaskStatus.OPEN,
            due = (currentTimestamp - 2.days).toLocalDateTime(timeZone).date.toString(),
            tags = listOf("personal")
        ),
        Task(
            id = "t-3",
            title = "Renew car registration",
            status = TaskStatus.DONE,
            due = null,
            tags = listOf("personal")
        )
    ),
    val places: List<Place> = listOf(
        Place(name = "Sushi Way", address = "120 Oak St", rating = 4.6),
        Place(name = "Borscht Hub", address = "115 Oak St", rating = 4.4),
        Place(name = "Llama Coffee", address = "123 Oak St", rating = 4.7),
    )
)

private fun getBirthday(now: Instant, timeZone: TimeZone, start: Int = 0, end: Int = 7): String {
    val randomDaysAhead = Random.nextInt(start, end)
    return (now + randomDaysAhead.days)
        .toLocalDateTime(timeZone)
        .date.toString()
}
