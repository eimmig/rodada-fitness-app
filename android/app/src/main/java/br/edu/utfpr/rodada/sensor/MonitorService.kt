package br.edu.utfpr.rodada.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.edu.utfpr.rodada.MainActivity
import br.edu.utfpr.rodada.R
import br.edu.utfpr.rodada.container
import br.edu.utfpr.rodada.data.Reading
import br.edu.utfpr.rodada.data.RoutePoint
import br.edu.utfpr.rodada.data.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Health (+ location) foreground service. While a session runs it:
 *  - reads the accelerometer (and the step counter when present), pattern from referencias/UsandoSensores;
 *  - records the GPS route, pattern from referencias/UsandoGPS;
 *  - every minute sends a reading with name, date/time and measured level;
 *  - on stop, saves the session as a Workout.
 */
class MonitorService : Service(), SensorEventListener, LocationListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var meter: ActivityMeter
    private var locationManager: LocationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var lastStepCounter: Float? = null
    private var pendingSteps = 0
    private var seconds = 0

    private var sessionStart = 0L
    private var sessionPoints = 0
    private var sessionSteps = 0
    private val route = mutableListOf<RoutePoint>()
    private var lastLocation: Location? = null
    private var distance = 0.0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        if (ticker == null) start()
        return START_STICKY
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun start() {
        createChannel()
        val canTrack = hasLocationPermission()
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            if (canTrack) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(0), type)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val accelerometer = linear ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        meter = ActivityMeter(removeGravity = linear == null)
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val canCountSteps = stepCounter != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
        if (canCountSteps) sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)

        if (canTrack) startLocation()

        // Without a wake lock the accelerometer stops delivering events once the screen is off.
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rodada:monitor")
            .apply { acquire(16 * 60 * 60 * 1000L) }

        sessionStart = System.currentTimeMillis()
        MonitorState.update {
            it.copy(active = true, stepsAvailable = canCountSteps, startedAtMillis = sessionStart, tracking = canTrack, distanceMeters = 0.0)
        }

        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                val level = meter.closeSecond()
                seconds++
                MonitorState.update {
                    it.copy(currentLevel = level, pendingPoints = meter.pendingPoints, pendingSteps = pendingSteps, distanceMeters = distance)
                }
                if (seconds % 60 == 0) upload()
            }
        }
    }

    @SuppressLint("MissingPermission") // Checked by hasLocationPermission() before calling.
    private fun startLocation() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = lm
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { lm.isProviderEnabled(it) }
            .forEach { lm.requestLocationUpdates(it, 3000L, 5f, this, Looper.getMainLooper()) }
    }

    override fun onLocationChanged(location: Location) {
        if (location.hasAccuracy() && location.accuracy > 50f) return
        lastLocation?.let { distance += it.distanceTo(location) }
        lastLocation = location
        synchronized(route) { route += RoutePoint(location.latitude, location.longitude) }
    }

    private suspend fun upload() {
        val user = container.auth.user.value ?: return
        val points = meter.takePoints()
        val steps = pendingSteps.also { pendingSteps = 0 }
        sessionPoints += points
        sessionSteps += steps
        if (points == 0 && steps == 0) return
        container.repository.sendReading(Reading(user.uid, user.name, System.currentTimeMillis(), points, steps))
        MonitorState.update { it.copy(pendingPoints = meter.pendingPoints, pendingSteps = 0) }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(points))
    }

    private suspend fun saveWorkout(): String? {
        val user = container.auth.user.value ?: return null
        val end = System.currentTimeMillis()
        if (end - sessionStart < 10_000) return null // Ignore accidental taps.
        val workout = Workout(
            id = UUID.randomUUID().toString(),
            uid = user.uid,
            name = sessionName(sessionStart),
            startMillis = sessionStart,
            endMillis = end,
            intensity = sessionPoints,
            steps = sessionSteps,
            distanceMeters = distance,
            route = synchronized(route) { route.toList() },
        )
        container.repository.saveWorkout(workout)
        return workout.id
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                // The counter is cumulative since boot: only the difference matters.
                val total = event.values[0]
                lastStepCounter?.let { pendingSteps += (total - it).toInt().coerceAtLeast(0) }
                lastStepCounter = total
            }
            else -> meter.sample(event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun stopEverything() {
        if (ticker == null) {
            stopSelf()
            return
        }
        ticker?.cancel()
        // Upload what is left of the current minute, save the session, then stop.
        scope.launch {
            upload()
            val id = saveWorkout()
            MonitorState.update { it.copy(finishedWorkoutId = id) }
            ServiceCompat.stopForeground(this@MonitorService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        locationManager?.removeUpdates(this)
        wakeLock?.takeIf { it.isHeld }?.release()
        ticker?.cancel()
        scope.cancel()
        MonitorState.update {
            it.copy(active = false, currentLevel = 0, pendingPoints = 0, pendingSteps = 0, startedAtMillis = 0L, tracking = false)
        }
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL, getString(R.string.channel_monitor), NotificationManager.IMPORTANCE_LOW)
            .apply { description = getString(R.string.channel_monitor_desc) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(lastMinute: Int) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_rodada)
        .setColor(ContextCompat.getColor(this, R.color.brand_primary))
        .setContentTitle("Atividade em andamento")
        .setContentText(if (lastMinute > 0) "Último minuto: +$lastMinute pontos de movimento" else "Medindo seu movimento")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setUsesChronometer(true)
        .setWhen(if (sessionStart > 0) sessionStart else System.currentTimeMillis())
        .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        .setContentIntent(
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        )
        .addAction(
            0, "Parar atividade",
            PendingIntent.getService(this, 1, Intent(this, MonitorService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        )
        .build()

    companion object {
        private const val CHANNEL = "monitor"
        private const val NOTIFICATION_ID = 46
        private const val ACTION_STOP = "br.edu.utfpr.rodada.STOP"

        fun sessionName(startMillis: Long): String {
            val hour = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).hour
            return when (hour) {
                in 5..11 -> "Atividade da manhã"
                in 12..17 -> "Atividade da tarde"
                else -> "Atividade da noite"
            }
        }

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))

        fun stop(context: Context) =
            context.startService(Intent(context, MonitorService::class.java).setAction(ACTION_STOP))
    }
}
