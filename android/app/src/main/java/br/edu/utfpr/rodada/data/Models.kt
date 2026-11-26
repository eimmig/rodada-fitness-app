package br.edu.utfpr.rodada.data

import java.time.LocalDate

data class User(
    val uid: String,
    val name: String,
    val photoUrl: String? = null,
)

/** A group is a league: each day of its period is one round. */
data class Group(
    val id: String,
    val name: String,
    val code: String,
    val startDate: LocalDate,
    /** null = open-ended challenge with no end date. */
    val durationDays: Int?,
    val intensityGoal: Int,
    val stepsGoal: Int,
    /** uid -> display name. */
    val members: Map<String, String>,
    val ownerUid: String,
) {
    val endDate: LocalDate? get() = durationDays?.let { startDate.plusDays(it.toLong() - 1) }
}

/** One user's totals for one day (`daily` collection). */
data class DailyActivity(
    val uid: String,
    val name: String,
    val day: LocalDate,
    val intensity: Int,
    val steps: Int,
)

/** One reading sent to the server (`readings` collection): name, date/time and measured level. */
data class Reading(
    val uid: String,
    val name: String,
    val timestampMillis: Long,
    val intensity: Int,
    val steps: Int,
)

data class RoutePoint(val lat: Double, val lng: Double)

/** One recorded activity session, from "Iniciar" to "Parar". */
data class Workout(
    val id: String,
    val uid: String,
    val name: String,
    val startMillis: Long,
    val endMillis: Long,
    val intensity: Int,
    val steps: Int,
    val distanceMeters: Double,
    val route: List<RoutePoint>,
) {
    val durationMillis: Long get() = endMillis - startMillis
}

enum class Zone { PODIUM, NEUTRAL, RELEGATION }

data class StandingRow(
    val uid: String,
    val name: String,
    val position: Int,
    /** Rounds won (days that counted). */
    val roundsWon: Int,
    val intensity: Int,
    /** Positions gained (+) or lost (-) with today's activity. */
    val movement: Int,
    val zone: Zone,
    val wonToday: Boolean,
)

data class RankingRow(
    val uid: String,
    val name: String,
    val position: Int,
    val intensity: Int,
    val steps: Int,
)
