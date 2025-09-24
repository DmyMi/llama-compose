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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val updatedAt: String
)

@Serializable
data class NoteSummary(
    val id: String,
    val title: String,
    val updatedAt: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val birthday: String,
    val emails: List<String> = emptyList(),
    val phones: List<String> = emptyList()
)

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val start: String,
    val end: String,
    val attendees: List<String> = emptyList(),
    val location: String? = null
)

@Serializable
enum class TaskStatus {
    @SerialName("open")
    OPEN,

    @SerialName("done")
    DONE
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val status: TaskStatus = TaskStatus.OPEN,
    val due: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class Place(
    val name: String,
    val address: String,
    val rating: Double? = null
)
