package br.edu.utfpr.rodada.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Remote server: Cloud Firestore (pattern from referencias/UsandoFirebaseCF-PM46S-2026-2).
 *
 * Collections:
 *  - users/{uid}            name, photo, totalIntensity, totalSteps
 *  - readings/{auto}        uid, name, timestamp, day, intensity, steps   (one per monitored minute)
 *  - daily/{uid_yyyy-MM-dd} uid, name, day, intensity, steps              (summed with increment)
 *  - groups/{id}            name, code, startDate, durationDays (null = no end), intensityGoal, stepsGoal,
 *                           members (uid array), memberNames (uid -> name), ownerUid
 *
 *  - workouts/{id}          uid, name, start, end, intensity, steps, distanceMeters, route [{lat, lng}]
 *
 * Firestore's offline cache is on by default: readings taken without network are queued.
 */
class FirestoreChallengeRepository : ChallengeRepository {
    private val db = Firebase.firestore
    override val isDemo = false

    override suspend fun registerUser(user: User) {
        db.collection(USERS).document(user.uid)
            .set(mapOf("name" to user.name, "photo" to user.photoUrl), SetOptions.merge())
            .await()
    }

    override fun observeMyGroups(uid: String): Flow<List<Group>> =
        db.collection(GROUPS).whereArrayContains("members", uid).snapshots { docs ->
            docs.mapNotNull { it.toGroup() }.sortedBy { it.name }
        }

    override fun observeStandings(group: Group): Flow<List<StandingRow>> {
        // whereIn accepts up to 30 values; class groups stay well below that.
        val uids = group.members.keys.take(30).ifEmpty { listOf("-") }
        return db.collection(DAILY).whereIn("uid", uids).snapshots { docs ->
            Rules.standings(group, docs.mapNotNull { it.toDaily() }, LocalDate.now())
        }
    }

    override fun observeToday(uid: String): Flow<DailyActivity?> = callbackFlow {
        val registration = db.collection(DAILY).document(dailyId(uid, LocalDate.now())).addSnapshotListener { snap, _ ->
            trySend(snap?.toDaily())
        }
        awaitClose { registration.remove() }
    }

    override fun observeMyDays(uid: String): Flow<List<DailyActivity>> =
        db.collection(DAILY).whereEqualTo("uid", uid).snapshots { docs -> docs.mapNotNull { it.toDaily() } }

    override fun observeRankingToday(): Flow<List<RankingRow>> =
        db.collection(DAILY).whereEqualTo("day", LocalDate.now().toString()).snapshots { docs ->
            Rules.ranking(docs.mapNotNull { it.toDaily() })
        }

    override fun observeRankingTotal(): Flow<List<RankingRow>> =
        db.collection(USERS).orderBy("totalIntensity", Query.Direction.DESCENDING).limit(100).snapshots { docs ->
            docs.mapIndexed { i, d ->
                RankingRow(
                    uid = d.id,
                    name = d.getString("name") ?: "Sem nome",
                    position = i + 1,
                    intensity = d.getLong("totalIntensity")?.toInt() ?: 0,
                    steps = d.getLong("totalSteps")?.toInt() ?: 0,
                )
            }
        }

    override suspend fun createGroup(
        user: User, name: String, durationDays: Int?, intensityGoal: Int, stepsGoal: Int,
    ): Group {
        val ref = db.collection(GROUPS).document()
        val group = Group(
            id = ref.id,
            name = name.trim(),
            code = Rules.newCode(),
            startDate = LocalDate.now(),
            durationDays = durationDays,
            intensityGoal = intensityGoal,
            stepsGoal = stepsGoal,
            members = mapOf(user.uid to user.name),
            ownerUid = user.uid,
        )
        ref.set(
            mapOf(
                "name" to group.name,
                "code" to group.code,
                "startDate" to group.startDate.toString(),
                "durationDays" to group.durationDays,
                "intensityGoal" to group.intensityGoal,
                "stepsGoal" to group.stepsGoal,
                "members" to listOf(user.uid),
                "memberNames" to group.members,
                "ownerUid" to user.uid,
            )
        ).await()
        return group
    }

    override suspend fun joinGroup(user: User, code: String): Group {
        val normalized = Rules.normalizeCode(code)
        val doc = db.collection(GROUPS).whereEqualTo("code", normalized).limit(1).get().await().documents.firstOrNull()
            ?: throw InvalidCodeException(normalized)
        doc.reference.update(
            mapOf(
                "members" to FieldValue.arrayUnion(user.uid),
                "memberNames.${user.uid}" to user.name,
            )
        ).await()
        val group = doc.toGroup() ?: throw InvalidCodeException(normalized)
        return group.copy(members = group.members + (user.uid to user.name))
    }

    override suspend fun sendReading(reading: Reading) {
        val instant = Instant.ofEpochMilli(reading.timestampMillis)
        val day = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val batch = db.batch()
        batch.set(
            db.collection(READINGS).document(),
            mapOf(
                "uid" to reading.uid,
                "name" to reading.name,
                "timestamp" to Timestamp(Date.from(instant)),
                "day" to day.toString(),
                "intensity" to reading.intensity,
                "steps" to reading.steps,
            )
        )
        batch.set(
            db.collection(DAILY).document(dailyId(reading.uid, day)),
            mapOf(
                "uid" to reading.uid,
                "name" to reading.name,
                "day" to day.toString(),
                "intensity" to FieldValue.increment(reading.intensity.toLong()),
                "steps" to FieldValue.increment(reading.steps.toLong()),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
        batch.set(
            db.collection(USERS).document(reading.uid),
            mapOf(
                "name" to reading.name,
                "totalIntensity" to FieldValue.increment(reading.intensity.toLong()),
                "totalSteps" to FieldValue.increment(reading.steps.toLong()),
            ),
            SetOptions.merge(),
        )
        // Not awaited: with the offline cache the commit stays queued until there is network.
        batch.commit()
    }

    override suspend fun saveWorkout(workout: Workout) {
        // Not awaited, same reason as readings. The photo is stored on the device (PhotoStore).
        db.collection(WORKOUTS).document(workout.id).set(
            mapOf(
                "uid" to workout.uid,
                "name" to workout.name,
                "start" to Timestamp(Date(workout.startMillis)),
                "end" to Timestamp(Date(workout.endMillis)),
                "intensity" to workout.intensity,
                "steps" to workout.steps,
                "distanceMeters" to workout.distanceMeters,
                "route" to workout.route.map { mapOf("lat" to it.lat, "lng" to it.lng) },
            )
        )
    }

    override fun observeMyWorkouts(uid: String): Flow<List<Workout>> =
        db.collection(WORKOUTS).whereEqualTo("uid", uid).snapshots { docs ->
            docs.mapNotNull { it.toWorkout() }.sortedByDescending { it.startMillis }
        }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toWorkout(): Workout? = runCatching {
        Workout(
            id = id,
            uid = getString("uid")!!,
            name = getString("name") ?: "Sem nome",
            startMillis = getTimestamp("start")!!.toDate().time,
            endMillis = getTimestamp("end")!!.toDate().time,
            intensity = getLong("intensity")?.toInt() ?: 0,
            steps = getLong("steps")?.toInt() ?: 0,
            distanceMeters = getDouble("distanceMeters") ?: 0.0,
            route = (get("route") as? List<Map<String, Any>>).orEmpty().mapNotNull { p ->
                val lat = (p["lat"] as? Number)?.toDouble()
                val lng = (p["lng"] as? Number)?.toDouble()
                if (lat != null && lng != null) RoutePoint(lat, lng) else null
            },
        )
    }.getOrNull()

    private fun <T> Query.snapshots(transform: (List<DocumentSnapshot>) -> T): Flow<T> = callbackFlow {
        val registration = addSnapshotListener { snap, _ -> if (snap != null) trySend(snap.documents) }
        awaitClose { registration.remove() }
    }.map(transform)

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toGroup(): Group? = runCatching {
        Group(
            id = id,
            name = getString("name")!!,
            code = getString("code")!!,
            startDate = LocalDate.parse(getString("startDate")!!),
            durationDays = getLong("durationDays")?.toInt(),
            intensityGoal = getLong("intensityGoal")?.toInt() ?: Rules.DEFAULT_INTENSITY_GOAL,
            stepsGoal = getLong("stepsGoal")?.toInt() ?: Rules.DEFAULT_STEPS_GOAL,
            members = (get("memberNames") as? Map<String, String>).orEmpty(),
            ownerUid = getString("ownerUid").orEmpty(),
        )
    }.getOrNull()

    private fun DocumentSnapshot.toDaily(): DailyActivity? = runCatching {
        DailyActivity(
            uid = getString("uid")!!,
            name = getString("name") ?: "Sem nome",
            day = LocalDate.parse(getString("day")!!),
            intensity = getLong("intensity")?.toInt() ?: 0,
            steps = getLong("steps")?.toInt() ?: 0,
        )
    }.getOrNull()

    private companion object {
        const val USERS = "users"
        const val READINGS = "readings"
        const val DAILY = "daily"
        const val GROUPS = "groups"
        const val WORKOUTS = "workouts"
        fun dailyId(uid: String, day: LocalDate) = "${uid}_$day"
    }
}
