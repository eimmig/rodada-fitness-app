package br.edu.utfpr.rodada.sensor

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Turns accelerometer samples into a movement level (0–10) per second and into points.
 * No Android dependency: takes x/y/z and returns numbers.
 *
 * - With TYPE_LINEAR_ACCELERATION gravity is already removed.
 * - With TYPE_ACCELEROMETER a low-pass filter estimates gravity and it is subtracted.
 */
class ActivityMeter(private val removeGravity: Boolean) {
    private val gravity = FloatArray(3)
    private var gravityReady = false
    private var sum = 0.0
    private var samples = 0
    private var points = 0.0

    fun sample(x: Float, y: Float, z: Float) {
        var ax = x; var ay = y; var az = z
        if (removeGravity) {
            if (!gravityReady) {
                gravity[0] = x; gravity[1] = y; gravity[2] = z
                gravityReady = true
            }
            gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x
            gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y
            gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z
            ax -= gravity[0]; ay -= gravity[1]; az -= gravity[2]
        }
        sum += sqrt((ax * ax + ay * ay + az * az).toDouble())
        samples++
    }

    /** Closes the current second: returns its 0–10 level and adds level/6 to pending points. */
    fun closeSecond(): Int {
        val mean = if (samples == 0) 0.0 else sum / samples
        sum = 0.0
        samples = 0
        val level = level(mean)
        points += level / 6.0
        return level
    }

    /** Points not sent yet (current minute). */
    val pendingPoints: Int get() = points.toInt()

    /** Takes the whole accumulated points out for sending. */
    fun takePoints(): Int {
        val whole = points.toInt()
        points -= whole
        return whole
    }

    companion object {
        private const val ALPHA = 0.8f
        /** Below this it is just the noise of a phone lying on a desk. */
        private const val DEAD_ZONE = 0.35
        private const val LEVEL_STEP = 0.35

        fun level(meanDynamicAcceleration: Double): Int =
            if (meanDynamicAcceleration < DEAD_ZONE) 0
            else floor(meanDynamicAcceleration / LEVEL_STEP).toInt().coerceIn(1, 10)
    }
}
