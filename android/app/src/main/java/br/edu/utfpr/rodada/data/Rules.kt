package br.edu.utfpr.rodada.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * League rules, kept pure so every screen computes them the same way.
 *
 * Intensity: every second the sensor yields a level from 0 to 10 (see ActivityMeter);
 * each second adds level/6 points, so one minute at the top level is worth 100 points.
 * Ordinary walking sits around level 4–7, i.e. 40–70 points per minute.
 */
object Rules {
    /** About 30 min of walking, the WHO daily minimum. */
    const val DEFAULT_INTENSITY_GOAL = 1800
    const val DEFAULT_STEPS_GOAL = 6000
    const val DEFAULT_DURATION_DAYS = 30
    const val CODE_LENGTH = 6

    /** No ambiguous characters (0/O, 1/I/L) so the code can be read out loud. */
    private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun newCode(random: Random = Random.Default): String =
        (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

    fun normalizeCode(text: String): String = text.uppercase().filter { it in CODE_ALPHABET }

    /** A day counts when intensity OR steps reach the goal. */
    fun counts(day: DailyActivity, intensityGoal: Int, stepsGoal: Int): Boolean =
        day.intensity >= intensityGoal || (day.steps > 0 && day.steps >= stepsGoal)

    /** Today's round (1-based), clamped to the group's period when it has one. */
    fun currentRound(group: Group, today: LocalDate): Int {
        val n = ChronoUnit.DAYS.between(group.startDate, today).toInt() + 1
        return group.durationDays?.let { n.coerceIn(0, it) } ?: n.coerceAtLeast(0)
    }

    fun isFinished(group: Group, today: LocalDate) = group.endDate?.let { today.isAfter(it) } ?: false

    /** "dia 7 de 30", "dia 7" (open-ended) or "encerrado". */
    fun dayLabel(group: Group, today: LocalDate): String = when {
        isFinished(group, today) -> "encerrado"
        group.durationDays == null -> "dia ${currentRound(group, today)}"
        else -> "dia ${currentRound(group, today)} de ${group.durationDays}"
    }

    /**
     * Builds the group table: sorted by rounds won, ties broken by accumulated intensity.
     * Movement compares against the table without today's activity.
     */
    fun standings(group: Group, days: List<DailyActivity>, today: LocalDate): List<StandingRow> {
        val until = group.endDate?.let { minOf(today, it) } ?: today
        val inPeriod = days.filter { it.uid in group.members && !it.day.isBefore(group.startDate) && !it.day.isAfter(until) }

        data class Totals(val uid: String, val name: String, val rounds: Int, val intensity: Int, val wonToday: Boolean)

        fun totals(list: List<DailyActivity>): List<Totals> = group.members.map { (uid, name) ->
            val mine = list.filter { it.uid == uid }
            Totals(
                uid = uid,
                name = name,
                rounds = mine.count { counts(it, group.intensityGoal, group.stepsGoal) },
                intensity = mine.sumOf { it.intensity },
                wonToday = mine.any { it.day == today && counts(it, group.intensityGoal, group.stepsGoal) },
            )
        }.sortedWith(compareByDescending<Totals> { it.rounds }.thenByDescending { it.intensity }.thenBy { it.name })

        val now = totals(inPeriod)
        val before = totals(inPeriod.filter { it.day != today }).mapIndexed { i, t -> t.uid to i + 1 }.toMap()
        val size = now.size

        return now.mapIndexed { i, t ->
            val position = i + 1
            StandingRow(
                uid = t.uid,
                name = t.name,
                position = position,
                roundsWon = t.rounds,
                intensity = t.intensity,
                movement = (before[t.uid] ?: position) - position,
                zone = zone(position, size),
                wonToday = t.wonToday,
            )
        }
    }

    /** Podium with 4+ members; bottom two are relegated with 6+. */
    fun zone(position: Int, total: Int): Zone = when {
        total >= 4 && position <= 3 -> Zone.PODIUM
        total >= 6 && position > total - 2 -> Zone.RELEGATION
        else -> Zone.NEUTRAL
    }

    /**
     * Consecutive achieved days ending today. If today is not achieved yet, the streak
     * still counts up to yesterday (it is only lost once today ends without activity).
     */
    fun streak(days: List<DailyActivity>, intensityGoal: Int, stepsGoal: Int, today: LocalDate): Int {
        val achieved = days.filter { counts(it, intensityGoal, stepsGoal) }.map { it.day }.toSet()
        var day = if (today in achieved) today else today.minusDays(1)
        var n = 0
        while (day in achieved) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    fun ranking(days: List<DailyActivity>): List<RankingRow> =
        days.groupBy { it.uid }
            .map { (uid, list) -> RankingRow(uid, list.first().name, 0, list.sumOf { it.intensity }, list.sumOf { it.steps }) }
            .sortedWith(compareByDescending<RankingRow> { it.intensity }.thenBy { it.name })
            .mapIndexed { i, row -> row.copy(position = i + 1) }
}
