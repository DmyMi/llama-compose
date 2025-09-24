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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

object FriendInfoTool : SimpleTool<FriendInfoTool.Args>() {
    @Serializable
    data class Args(val friendName: String) : ToolArgs

    override val argsSerializer: KSerializer<Args> = Args.serializer()

    override val descriptor: ToolDescriptor = ToolDescriptor(
        name = "find_friend_details",
        description = "Returns information about a friend including birthday, sports preferences, and personal details",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "friendName",
                description = "The name of the friend to search for",
                type = ToolParameterType.String
            ),
        ),
    )

    override suspend fun doExecute(args: Args): String {
        val json = Json { ignoreUnknownKeys = true }

        val timeZone = TimeZone.currentSystemDefault()
        val randomDaysAhead = Random.nextInt(0, 7)
        val birthdayDate = (Clock.System.now() + randomDaysAhead.days)
            .toLocalDateTime(timeZone)
            .date

        val sports = listOf(
            "football", "basketball", "tennis", "swimming", "running",
            "cycling", "boxing", "golf", "volleyball", "badminton"
        )
        val shuffledSports = sports.shuffled()
        val likes = shuffledSports.take(3)
        val dislikes = shuffledSports.drop(3).take(2)

        val smallNotesOptions = listOf(
            "Met through mutual friends; kind and upbeat.",
            "Enjoys good coffee and weekend hikes.",
            "Recently got into photography and board games.",
            "Works remotely; loves travel and trying new cuisines.",
            "We catch up often; always supportive and thoughtful."
        )
        val smallNote = smallNotesOptions.random()

        val notes = """
${args.friendName} — notes
Birthday: $birthdayDate
Likes sports: ${likes.joinToString(", ")}
Dislikes sports: ${dislikes.joinToString(", ")}
Birthday wish: Labubu
$smallNote
""".trimIndent()

        val result = FriendNotes(notes)

        return json.encodeToString(result)
    }
}

@Serializable
data class FriendNotes(
    val notes: String,
)
