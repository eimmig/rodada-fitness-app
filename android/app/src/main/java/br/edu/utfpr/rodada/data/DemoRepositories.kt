package br.edu.utfpr.rodada.data

import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/*
 * Demo mode: runs when there is no google-services.json.
 * Every name and number here is FICTITIOUS and the UI shows a "Demonstração" banner.
 * Nothing is sent to any server.
 */

private val DEMO_USER = User(uid = "demo-you", name = "Você")

class DemoAuthRepository : AuthRepository {
    private val _user = MutableStateFlow<User?>(null)
    override val user: StateFlow<User?> = _user.asStateFlow()

    override val providers = SignInProvider.entries

    override suspend fun signIn(provider: SignInProvider, activity: ComponentActivity): Result<User> {
        delay(400)
        _user.value = DEMO_USER
        return Result.success(DEMO_USER)
    }

    override fun signOut() {
        _user.value = null
    }
}

class DemoChallengeRepository : ChallengeRepository {
    override val isDemo = true

    private val today = LocalDate.now()

    private val fakeMembers = listOf(
        "demo-1" to "Ana Exemplo",
        "demo-2" to "Bruno Exemplo",
        "demo-3" to "Carla Exemplo",
        "demo-4" to "Diego Exemplo",
        "demo-5" to "Elisa Exemplo",
        "demo-6" to "Fábio Exemplo",
        "demo-7" to "Gabi Exemplo",
    )

    private val groups = MutableStateFlow(
        listOf(
            Group(
                id = "demo-group",
                name = "Turma de exemplo",
                code = "K7Q2MX",
                startDate = today.minusDays(6),
                durationDays = Rules.DEFAULT_DURATION_DAYS,
                intensityGoal = Rules.DEFAULT_INTENSITY_GOAL,
                stepsGoal = Rules.DEFAULT_STEPS_GOAL,
                members = (fakeMembers + (DEMO_USER.uid to DEMO_USER.name)).toMap(),
                ownerUid = "demo-1",
            )
        )
    )

    private val days = MutableStateFlow(generateHistory())

    private val workouts = MutableStateFlow(generateWorkouts())

    /** Two fictitious past sessions with made-up loops near the Curitiba campus. */
    private fun generateWorkouts(): List<Workout> {
        fun loop(centerLat: Double, centerLng: Double, radius: Double, seed: Int): List<RoutePoint> {
            val r = Random(seed)
            return (0..80).map { i ->
                val a = i / 80.0 * 2 * Math.PI
                val wobble = 1 + 0.25 * kotlin.math.sin(a * 3 + seed) + r.nextDouble(-0.04, 0.04)
                RoutePoint(centerLat + radius * wobble * kotlin.math.sin(a), centerLng + radius * 1.1 * wobble * kotlin.math.cos(a))
            }
        }
        val dayStart = today.atStartOfDay(ZoneId.systemDefault())
        val first = dayStart.minusDays(1).plusHours(7).plusMinutes(10).toInstant().toEpochMilli()
        val second = dayStart.minusDays(2).plusHours(18).plusMinutes(30).toInstant().toEpochMilli()
        return listOf(
            Workout("demo-w1", DEMO_USER.uid, "Atividade da manhã", first, first + 38 * 60_000, 2087, 5120, 3420.0, loop(-25.4395, -49.2687, 0.0045, 3)),
            Workout("demo-w2", DEMO_USER.uid, "Atividade da noite", second, second + 52 * 60_000, 2311, 6480, 4870.0, loop(-25.4282, -49.2671, 0.0060, 7)),
        )
    }

    override suspend fun saveWorkout(workout: Workout) {
        workouts.value = listOf(workout) + workouts.value.filter { it.id != workout.id }
    }

    override fun observeMyWorkouts(uid: String): Flow<List<Workout>> =
        workouts.map { list -> list.filter { it.uid == uid }.sortedByDescending { it.startMillis } }

    private fun generateHistory(): List<DailyActivity> {
        val random = Random(46)
        // How likely each fake member is to reach the goal on a given day.
        val effort = listOf(0.95, 0.85, 0.7, 0.6, 0.45, 0.3, 0.15)
        val list = mutableListOf<DailyActivity>()
        for (d in 6 downTo 1) {
            val day = today.minusDays(d.toLong())
            fakeMembers.forEachIndexed { i, (uid, name) ->
                val hit = random.nextDouble() < effort[i]
                val intensity = if (hit) 1800 + random.nextInt(900) else 300 + random.nextInt(1200)
                list += DailyActivity(uid, name, day, intensity, intensity * 3 + random.nextInt(800))
            }
            // "You" won 4 of the previous 6 rounds.
            val won = d != 3 && d != 5
            list += DailyActivity(DEMO_USER.uid, DEMO_USER.name, day, if (won) 2050 + d * 37 else 1240, if (won) 6400 else 3900)
        }
        // Today's partial values.
        fakeMembers.forEachIndexed { i, (uid, name) ->
            val v = listOf(1910, 1520, 990, 1830, 640, 410, 120)[i]
            list += DailyActivity(uid, name, today, v, v * 3)
        }
        list += DailyActivity(DEMO_USER.uid, DEMO_USER.name, today, 1236, 3412)
        return list
    }

    override suspend fun registerUser(user: User) = Unit

    override fun observeMyGroups(uid: String): Flow<List<Group>> =
        groups.map { list -> list.filter { uid in it.members }.sortedBy { it.name } }

    override fun observeStandings(group: Group): Flow<List<StandingRow>> =
        days.map { Rules.standings(groups.value.firstOrNull { it.id == group.id } ?: group, it, LocalDate.now()) }

    override fun observeToday(uid: String): Flow<DailyActivity?> =
        days.map { list -> list.firstOrNull { it.uid == uid && it.day == LocalDate.now() } }

    override fun observeMyDays(uid: String): Flow<List<DailyActivity>> =
        days.map { list -> list.filter { it.uid == uid } }

    override fun observeRankingToday(): Flow<List<RankingRow>> =
        days.map { list -> Rules.ranking(list.filter { it.day == LocalDate.now() }) }

    override fun observeRankingTotal(): Flow<List<RankingRow>> = days.map { Rules.ranking(it) }

    override suspend fun createGroup(
        user: User, name: String, durationDays: Int?, intensityGoal: Int, stepsGoal: Int,
    ): Group {
        delay(300)
        val group = Group(
            id = "demo-${System.nanoTime()}",
            name = name.trim(),
            code = Rules.newCode(),
            startDate = LocalDate.now(),
            durationDays = durationDays,
            intensityGoal = intensityGoal,
            stepsGoal = stepsGoal,
            members = mapOf(user.uid to user.name),
            ownerUid = user.uid,
        )
        groups.value = groups.value + group
        return group
    }

    override suspend fun joinGroup(user: User, code: String): Group {
        delay(300)
        val normalized = Rules.normalizeCode(code)
        val group = groups.value.firstOrNull { it.code == normalized } ?: throw InvalidCodeException(normalized)
        val updated = group.copy(members = group.members + (user.uid to user.name))
        groups.value = groups.value.map { if (it.id == group.id) updated else it }
        return updated
    }

    override suspend fun sendReading(reading: Reading) {
        val day = Instant.ofEpochMilli(reading.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val current = days.value
        val existing = current.firstOrNull { it.uid == reading.uid && it.day == day }
        days.value = if (existing == null) {
            current + DailyActivity(reading.uid, reading.name, day, reading.intensity, reading.steps)
        } else {
            current.map {
                if (it === existing) it.copy(intensity = it.intensity + reading.intensity, steps = it.steps + reading.steps) else it
            }
        }
    }
}
