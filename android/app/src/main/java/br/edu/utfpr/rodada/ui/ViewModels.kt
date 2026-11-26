package br.edu.utfpr.rodada.ui

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.utfpr.rodada.AppContainer
import br.edu.utfpr.rodada.data.DailyActivity
import br.edu.utfpr.rodada.data.Group
import br.edu.utfpr.rodada.data.InvalidCodeException
import br.edu.utfpr.rodada.data.RankingRow
import br.edu.utfpr.rodada.data.SignInCancelledException
import br.edu.utfpr.rodada.data.SignInProvider
import br.edu.utfpr.rodada.data.Rules
import br.edu.utfpr.rodada.data.StandingRow
import br.edu.utfpr.rodada.data.User
import br.edu.utfpr.rodada.data.Workout
import br.edu.utfpr.rodada.sensor.Monitor
import br.edu.utfpr.rodada.sensor.MonitorState
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------- Session ----------

data class SessionUiState(
    val user: User? = null,
    /** Provider whose flow is open right now, or null. */
    val signingIn: SignInProvider? = null,
    val error: String? = null,
    val isDemo: Boolean = false,
    val providers: List<SignInProvider> = listOf(SignInProvider.GOOGLE),
)

class SessionViewModel(private val c: AppContainer) : ViewModel() {
    private val signingIn = MutableStateFlow<SignInProvider?>(null)
    private val error = MutableStateFlow<String?>(null)

    val ui: StateFlow<SessionUiState> = combine(c.auth.user, signingIn, error) { user, busy, err ->
        SessionUiState(user, busy, err, c.repository.isDemo, c.auth.providers)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionUiState(c.auth.user.value, isDemo = c.repository.isDemo, providers = c.auth.providers))

    fun signIn(provider: SignInProvider, activity: ComponentActivity) {
        if (signingIn.value != null) return
        viewModelScope.launch {
            signingIn.value = provider
            error.value = null
            c.auth.signIn(provider, activity)
                .onSuccess { c.repository.registerUser(it) }
                .onFailure { if (it !is SignInCancelledException) error.value = signInMessage(provider, it) }
            signingIn.value = null
        }
    }

    fun signOut() = c.auth.signOut()

    private fun signInMessage(provider: SignInProvider, e: Throwable): String {
        val code = (e as? FirebaseAuthException)?.errorCode
        return when {
            e::class.simpleName == "NoCredentialException" ->
                "Nenhuma conta Google neste aparelho. Adicione uma em Configurações › Contas."
            code == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Esse e-mail já entrou por outro provedor. Use o mesmo botão da primeira vez."
            code == "ERROR_OPERATION_NOT_ALLOWED" ->
                "Login com ${provider.label} não está ativado no Firebase."
            else -> "Não deu para entrar com ${provider.label}: ${e.message ?: "erro desconhecido"}. Confira a conexão e tente de novo."
        }
    }
}

// ---------- Challenge ----------

data class HomeUiState(
    val user: User,
    val isDemo: Boolean,
    val loading: Boolean = true,
    val groups: List<Group> = emptyList(),
    val group: Group? = null,
    val standings: List<StandingRow> = emptyList(),
    val today: DailyActivity? = null,
    val myDays: List<DailyActivity> = emptyList(),
    val monitor: Monitor = Monitor(),
    val rankingToday: List<RankingRow> = emptyList(),
    val rankingTotal: List<RankingRow> = emptyList(),
    val workouts: List<Workout> = emptyList(),
) {
    val intensityGoal: Int get() = group?.intensityGoal ?: Rules.DEFAULT_INTENSITY_GOAL
    val stepsGoal: Int get() = group?.stepsGoal ?: Rules.DEFAULT_STEPS_GOAL

    /** Today's intensity including the minute not uploaded yet. */
    val intensityToday: Int get() = (today?.intensity ?: 0) + monitor.pendingPoints
    val stepsToday: Int get() = (today?.steps ?: 0) + monitor.pendingSteps

    val achievedToday: Boolean
        get() = intensityToday >= intensityGoal || (stepsToday > 0 && stepsToday >= stepsGoal)

    val streak: Int
        get() {
            val todayLive = DailyActivity(user.uid, user.name, LocalDate.now(), intensityToday, stepsToday)
            val days = myDays.filter { it.day != LocalDate.now() } + todayLive
            return Rules.streak(days, intensityGoal, stepsGoal, LocalDate.now())
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c: AppContainer, private val user: User) : ViewModel() {
    private val repo = c.repository
    private val focusedId = MutableStateFlow(c.preferences.focusedGroupId)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val groups = repo.observeMyGroups(user.uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val group = combine(groups, focusedId) { list, id ->
        list?.let { l -> l.firstOrNull { it.id == id } ?: l.firstOrNull() }
    }

    private val standings = group.flatMapLatest { g -> if (g == null) flowOf(emptyList()) else repo.observeStandings(g) }
        .onStart { emit(emptyList()) }

    private val ranking = combine(
        repo.observeRankingToday().onStart { emit(emptyList()) },
        repo.observeRankingTotal().onStart { emit(emptyList()) },
    ) { today, total -> today to total }

    private val mine = combine(
        repo.observeToday(user.uid).onStart { emit(null) },
        repo.observeMyDays(user.uid).onStart { emit(emptyList()) },
        repo.observeMyWorkouts(user.uid).onStart { emit(emptyList()) },
    ) { today, days, workouts -> Triple(today, days, workouts) }

    val ui: StateFlow<HomeUiState> = combine(
        combine(groups, group, standings) { gs, g, s -> Triple(gs, g, s) },
        mine,
        MonitorState.state,
        ranking,
    ) { (gs, g, s), (today, days, workouts), monitor, (rt, rTotal) ->
        HomeUiState(
            user = user,
            isDemo = repo.isDemo,
            loading = gs == null,
            groups = gs.orEmpty(),
            group = g,
            standings = s,
            today = today,
            myDays = days,
            monitor = monitor,
            rankingToday = rt,
            rankingTotal = rTotal,
            workouts = workouts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(user, repo.isDemo))

    /** Bumps whenever a photo is saved or removed. */
    val photoVersion: StateFlow<Int> = c.photos.version

    suspend fun loadPhoto(workoutId: String): ByteArray? = c.photos.load(workoutId)

    fun savePhoto(workoutId: String, jpeg: ByteArray) {
        viewModelScope.launch {
            runCatching { c.photos.save(workoutId, jpeg) }
                .onSuccess { _messages.tryEmit("Foto salva na atividade") }
                .onFailure { _messages.tryEmit("Não deu para salvar a foto. Tente de novo.") }
        }
    }

    fun removePhoto(workoutId: String) {
        viewModelScope.launch { c.photos.delete(workoutId) }
    }

    fun focus(groupId: String) {
        focusedId.value = groupId
        c.preferences.focusedGroupId = groupId
    }

    fun createGroup(name: String, durationDays: Int?, intensityGoal: Int, stepsGoal: Int, onCreated: (Group) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.createGroup(user, name, durationDays, intensityGoal, stepsGoal) }
                .onSuccess { focus(it.id); onCreated(it) }
                .onFailure { _messages.tryEmit("Não deu para criar o grupo. Confira a conexão e tente de novo.") }
        }
    }

    fun joinGroup(code: String, onJoined: (Group) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.joinGroup(user, code) }
                .onSuccess { focus(it.id); _messages.tryEmit("Você entrou em ${it.name}"); onJoined(it) }
                .onFailure {
                    onError(
                        if (it is InvalidCodeException) "Nenhum grupo com esse código. Confira as 6 letras com quem convidou."
                        else "Não deu para entrar agora. Confira a conexão e tente de novo."
                    )
                }
        }
    }

    val permissionsExplained: Boolean get() = c.preferences.permissionsExplained
    fun markPermissionsExplained() { c.preferences.permissionsExplained = true }
}
