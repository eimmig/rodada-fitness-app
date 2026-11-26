package br.edu.utfpr.rodada.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Monitor(
    val active: Boolean = false,
    /** Movement level of the last second, 0–10. */
    val currentLevel: Int = 0,
    /** Measured points not uploaded yet (current minute). */
    val pendingPoints: Int = 0,
    val pendingSteps: Int = 0,
    /** false when the device has no step counter or the permission was denied. */
    val stepsAvailable: Boolean = true,
    /** When the current session started, for the elapsed timer. */
    val startedAtMillis: Long = 0L,
    /** GPS state for the current session. */
    val tracking: Boolean = false,
    val distanceMeters: Double = 0.0,
    /** Set once when a session ends, so the UI can open its summary. */
    val finishedWorkoutId: String? = null,
)

/** Bridge between MonitorService and the UI. */
object MonitorState {
    private val _state = MutableStateFlow(Monitor())
    val state: StateFlow<Monitor> = _state.asStateFlow()

    internal fun update(transform: (Monitor) -> Monitor) {
        _state.value = transform(_state.value)
    }

    /** The UI calls this after navigating to the finished workout. */
    fun consumeFinishedWorkout() = update { it.copy(finishedWorkoutId = null) }
}
