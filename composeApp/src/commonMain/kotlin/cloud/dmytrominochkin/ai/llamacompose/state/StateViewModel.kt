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
package cloud.dmytrominochkin.ai.llamacompose.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.CalendarEvent
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.Contact
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.Note
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.Place
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.Task
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.TaskStatus
import cloud.dmytrominochkin.ai.llamacompose.agent.tools.assistantMockFlow
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.calendar_events
import cloud.dmytrominochkin.ai.llamacompose.resources.contacts
import cloud.dmytrominochkin.ai.llamacompose.resources.notes
import cloud.dmytrominochkin.ai.llamacompose.resources.places
import cloud.dmytrominochkin.ai.llamacompose.resources.tasks
import cloud.dmytrominochkin.ai.llamacompose.runAsync
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString

data class FormattedAssistantState(
    val title: String,
    val content: String,
)

class StateViewModel : ViewModel() {

    val uiState = assistantMockFlow
        .map {
            listOf(
                runAsync(viewModelScope.coroutineContext) { formatCalendarEvents(it.calendarEvents) },
                runAsync(viewModelScope.coroutineContext) { formatContacts(it.contacts) },
                runAsync(viewModelScope.coroutineContext) { formatNotes(it.notes) },
                runAsync(viewModelScope.coroutineContext) { formatTasks(it.tasks) },
                runAsync(viewModelScope.coroutineContext) { formatPlaces(it.places) },
            ).awaitAll()
        }

    suspend fun formatCalendarEvents(calendarEvents: List<CalendarEvent>): FormattedAssistantState {
        val content = if (calendarEvents.isEmpty()) {
            "- _No events_"
        } else {
            calendarEvents.map { e ->
                val time = "${e.start} → ${e.end}"
                val location = e.location?.let { " at $it" } ?: ""
                val attendees = if (e.attendees.isNotEmpty()) e.attendees.joinToString(", ") else ""
                "- ${e.title} - $time$location\n  - attendees: $attendees"
            }.fold("") { acc, e -> acc + "\n$e" }
        }
        return FormattedAssistantState(
            title = getString(Res.string.calendar_events),
            content = content.trimEnd()
        )
    }

    suspend fun formatContacts(contacts: List<Contact>): FormattedAssistantState {
        val content = if (contacts.isEmpty()) {
            "- _No contacts_"
        } else {
            contacts.map { c ->
                val emails = if (c.emails.isNotEmpty()) c.emails.joinToString(", ") else null
                val phones = if (c.phones.isNotEmpty()) c.phones.joinToString(", ") else null
                "- ${c.name}\n  - birthday: ${c.birthday}\n  - emails: $emails\n  - phones: $phones"
            }.fold("") { acc, e -> acc + "\n$e" }
        }
        return FormattedAssistantState(
            title = getString(Res.string.contacts),
            content = content.trimEnd()
        )
    }

    suspend fun formatNotes(notes: List<Note>): FormattedAssistantState {
        val content = if (notes.isEmpty()) {
            "- _No notes_"
        } else {
            notes.map { n ->
                val tags = if (n.tags.isNotEmpty()) " [tags: ${n.tags.joinToString(", ")}]" else ""
                "- ${n.title} - updated ${n.updatedAt}$tags\n  - ${n.content}"
            }.fold("") { acc, e -> acc + "\n$e" }
        }
        return FormattedAssistantState(
            title = getString(Res.string.notes),
            content = content.trimEnd()
        )
    }

    suspend fun formatTasks(tasks: List<Task>): FormattedAssistantState {
        val content = if (tasks.isEmpty()) {
            "- _No tasks_"
        } else {
            tasks.map { t ->
                val checkbox = if (t.status == TaskStatus.DONE) "[x]" else "[ ]"
                val due = t.due?.let { " (due: $it)" } ?: ""
                val tags = if (t.tags.isNotEmpty()) " [tags: ${t.tags.joinToString(", ")}]" else ""
                "- $checkbox ${t.title}$due$tags"
            }.fold("") { acc, e -> acc + "\n$e" }
        }
        return FormattedAssistantState(
            title = getString(Res.string.tasks),
            content = content.trimEnd()
        )
    }

    suspend fun formatPlaces(places: List<Place>): FormattedAssistantState {
        val content = if (places.isEmpty()) {
            "- _No places_"
        } else {
            places.map { p ->
                "- ${p.name} - ${p.address} (rating: ${p.rating})"
            }.fold("") { acc, e -> acc + "\n$e" }
        }
        return FormattedAssistantState(
            title = getString(Res.string.places),
            content = content.trimEnd()
        )
    }
}
