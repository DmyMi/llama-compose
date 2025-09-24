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
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.math.min

object NotesSearchTool : Tool<NotesSearchTool.Args, NotesSearchTool.Result>() {
    @Serializable
    data class Args(
        val query: String = "",
        val limit: Int = 10,
        val tags: List<String> = emptyList()
    ) : ToolArgs

    @Serializable
    data class Result(
        val notes: List<NoteSummary>
    ) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "notes_search",
        description = "Search user notes by keywords/tags. Returns short summaries.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "query",
                description = "Query string to search in title and content.",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "limit",
                description = "Maximum number of results to return.",
                type = ToolParameterType.Integer
            )
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                name = "tags",
                description = "List of tags to filter the search.",
                type = ToolParameterType.List(ToolParameterType.String)
            ),
        )
    )

    override suspend fun execute(args: Args): Result {
        val q = args.query.trim()
        val filterByTags = args.tags.map { it.lowercase() }.toSet()
        val results = assistantMockFlow.value.notes
            .asSequence()
            .filter { note ->
                val base = if (q.isBlank()) true else note.title.contains(q, ignoreCase = true) || note.content.contains(q, ignoreCase = true)
                val tagsOk = if (filterByTags.isEmpty()) true else note.tags.any { it.lowercase() in filterByTags }
                base && tagsOk
            }
            .sortedByDescending { note ->
                try { Instant.parse(note.updatedAt) } catch (_: Exception) { Instant.DISTANT_PAST }
            }
            .map { note -> NoteSummary(id = note.id, title = note.title, updatedAt = note.updatedAt, tags = note.tags) }
            .toList()

        return Result(notes = results.take(min(results.size, args.limit)))
    }
}

object NoteReadTool : Tool<NoteReadTool.Args, NoteReadTool.Result>() {
    @Serializable
    data class Args(val noteId: String) : ToolArgs

    @Serializable
    data class Result(val id: String, val title: String, val content: String, val updatedAt: String, val tags: List<String>) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "note_read",
        description = "Read a specific note by its id.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "noteId",
                description = "The id of the note to read.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val note = assistantMockFlow.value.notes.firstOrNull { it.id == args.noteId }
            ?: Note(id = args.noteId, title = "Not found", content = "", tags = emptyList(), updatedAt = "")
        return Result(note.id, note.title, note.content, note.updatedAt, note.tags)
    }
}

object NoteCreateTool : Tool<NoteCreateTool.Args, NoteCreateTool.Result>() {
    @Serializable
    data class Args(
        val title: String,
        val content: String,
        val tags: List<String> = emptyList()
    ) : ToolArgs

    @Serializable
    data class Result(val createdId: String) : ToolResult.JSONSerializable<Result> {
        override fun getSerializer(): KSerializer<Result> = serializer()
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "note_create",
        description = "Create a new note with title/content and optional tags.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "title",
                description = "Title for the new note.",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "content",
                description = "Content for the new note.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val id = createNote(args.title, args.content, args.tags)
        return Result(createdId = id)
    }
}
