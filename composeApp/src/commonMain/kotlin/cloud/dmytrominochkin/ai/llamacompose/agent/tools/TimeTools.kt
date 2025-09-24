/**
* Time tools by JetBrains
*/
package cloud.dmytrominochkin.ai.llamacompose.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

private val UTC_ZONE = TimeZone.UTC

/**
 * Tool for getting the current date and time
 */
object CurrentDatetimeTool : Tool<CurrentDatetimeTool.Args, CurrentDatetimeTool.Result>() {
    @Serializable
    data class Args(
        val timezone: String = "UTC"
    ) : ToolArgs

    @Serializable
    data class Result(
        val datetime: String,
        val date: String,
        val time: String,
        val timezone: String
    ) : ToolResult {
        override fun toStringDefault(): String {
            return "Current datetime: $datetime, Date: $date, Time: $time, Timezone: $timezone"
        }
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "current_datetime",
        description = "Get the current date and time in the specified timezone",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "timezone",
                description = "The timezone to get the current date and time in (e.g., 'UTC', 'Europe/Kyiv', 'America/New_York'). Defaults to UTC.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val zoneId = try {
            TimeZone.of(args.timezone)
        } catch (_: Exception) {
            UTC_ZONE
        }

        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(zoneId)
        val offset = zoneId.offsetAt(now)

        val time = localDateTime.time
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}:${time.second.toString().padStart(2, '0')}"

        return Result(
            datetime = "${localDateTime.date}T$timeStr$offset",
            date = localDateTime.date.toString(),
            time = timeStr,
            timezone = zoneId.id
        )
    }
}

/**
 * Tool for adding a duration to a date
 */
object AddDatetimeTool : Tool<AddDatetimeTool.Args, AddDatetimeTool.Result>() {
    @Serializable
    data class Args(
        val date: String,
        val days: Int,
        val hours: Int,
        val minutes: Int
    ) : ToolArgs

    @Serializable
    data class Result(
        val date: String,
        val originalDate: String,
        val daysAdded: Int,
        val hoursAdded: Int,
        val minutesAdded: Int
    ) : ToolResult {
        override fun toStringDefault(): String {
            return buildString {
                append("Date: $date")
                if (originalDate.isBlank()) {
                    append(" (starting from today)")
                } else {
                    append(" (starting from $originalDate)")
                }

                if (daysAdded != 0 || hoursAdded != 0 || minutesAdded != 0) {
                    append(" after adding")

                    if (daysAdded != 0) {
                        append(" $daysAdded days")
                    }

                    if (hoursAdded != 0) {
                        if (daysAdded != 0) append(",")
                        append(" $hoursAdded hours")
                    }

                    if (minutesAdded != 0) {
                        if (daysAdded != 0 || hoursAdded != 0) append(",")
                        append(" $minutesAdded minutes")
                    }
                }
            }
        }
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "add_datetime",
        description = "Add a duration to a date. Use this tool when you need to calculate offsets, such as tomorrow, in two days, etc.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "date",
                description = "The date to add to in ISO format (e.g., '2025-05-20')",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "days",
                description = "The number of days to add",
                type = ToolParameterType.Integer
            ),
            ToolParameterDescriptor(
                name = "hours",
                description = "The number of hours to add",
                type = ToolParameterType.Integer
            ),
            ToolParameterDescriptor(
                name = "minutes",
                description = "The number of minutes to add",
                type = ToolParameterType.Integer
            )
        )
    )

    override suspend fun execute(args: Args): Result {
        val baseDate = if (args.date.isNotBlank()) {
            try {
                LocalDate.parse(args.date)
            } catch (_: Exception) {
                // Use current date if parsing fails
                Clock.System.now().toLocalDateTime(UTC_ZONE).date
            }
        } else {
            Clock.System.now().toLocalDateTime(UTC_ZONE).date
        }

        // Convert to LocalDateTime to handle hours and minutes
        val baseDateTime = LocalDateTime(baseDate.year, baseDate.month, baseDate.dayOfMonth, 0, 0)
        val baseInstant = baseDateTime.toInstant(UTC_ZONE)

        val period = DateTimePeriod(
            days = args.days,
            hours = args.hours,
            minutes = args.minutes
        )

        val newInstant = baseInstant.plus(period, UTC_ZONE)
        val resultDate = newInstant.toLocalDateTime(UTC_ZONE).date.toString()

        return Result(
            date = resultDate,
            originalDate = args.date,
            daysAdded = args.days,
            hoursAdded = args.hours,
            minutesAdded = args.minutes
        )
    }
}
