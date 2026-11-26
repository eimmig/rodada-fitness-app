package br.edu.utfpr.rodada.data

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val user: StateFlow<User?>

    /** Providers that can be offered on the login screen in this build. */
    val providers: List<SignInProvider>

    /** Opens the provider's sign-in flow. Needs the running Activity. */
    suspend fun signIn(provider: SignInProvider, activity: ComponentActivity): Result<User>
    fun signOut()
}

enum class SignInProvider(val label: String) {
    GOOGLE("Google"),
    FACEBOOK("Facebook"),
    MICROSOFT("Microsoft"),
}

/** The user closed the sign-in flow. Not an error worth a message. */
class SignInCancelledException : Exception("Sign-in cancelled")

interface ChallengeRepository {
    /** true when data is fictitious (no google-services.json). */
    val isDemo: Boolean

    suspend fun registerUser(user: User)
    fun observeMyGroups(uid: String): Flow<List<Group>>
    fun observeStandings(group: Group): Flow<List<StandingRow>>
    fun observeToday(uid: String): Flow<DailyActivity?>
    /** Every day this user has activity for (feeds the challenge-days strip and the streak). */
    fun observeMyDays(uid: String): Flow<List<DailyActivity>>
    fun observeRankingToday(): Flow<List<RankingRow>>
    fun observeRankingTotal(): Flow<List<RankingRow>>

    suspend fun createGroup(user: User, name: String, durationDays: Int?, intensityGoal: Int, stepsGoal: Int): Group

    /** @throws InvalidCodeException when no group has this code. */
    suspend fun joinGroup(user: User, code: String): Group

    /** Stores the reading and adds it to the user's daily and lifetime totals. */
    suspend fun sendReading(reading: Reading)

    suspend fun saveWorkout(workout: Workout)
    fun observeMyWorkouts(uid: String): Flow<List<Workout>>
}

class InvalidCodeException(code: String) : Exception("No group with code $code")
