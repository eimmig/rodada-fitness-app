package br.edu.utfpr.rodada.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.Workout
import br.edu.utfpr.rodada.ui.components.RouteCanvas
import br.edu.utfpr.rodada.ui.components.RouteMap
import br.edu.utfpr.rodada.ui.components.SectionCard
import br.edu.utfpr.rodada.ui.components.fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")
private val dateTime = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", ptBr)

fun Workout.whenText(): String =
    Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).format(dateTime).replaceFirstChar { it.uppercase() }

fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

fun formatDistance(meters: Double): String =
    if (meters >= 1000) String.format(ptBr, "%.1f km", meters / 1000) else "${meters.toInt()} m"

/** Loads a stored photo, reloading whenever [version] changes. */
@Composable
fun rememberPhoto(workoutId: String, version: Int, load: suspend (String) -> ByteArray?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, workoutId, version) {
        value = load(workoutId)?.let { bytes ->
            withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
        }
    }
    return bitmap
}

private fun Bitmap.toJpeg(): ByteArray {
    // Keep the BLOB small: longest side at most 1280px.
    val scale = minOf(1f, 1280f / maxOf(width, height))
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true) else this
    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
}

private fun decodeUri(context: android.content.Context, uri: Uri): Bitmap? =
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

/** Summary of one workout: animated route, stats and the optional photo. */
@Composable
fun WorkoutScreen(
    workout: Workout,
    justFinished: Boolean,
    photo: ImageBitmap?,
    contentPadding: PaddingValues,
    onSavePhoto: (ByteArray) -> Unit,
    onRemovePhoto: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { onSavePhoto(it.toJpeg()) }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { decodeUri(context, it)?.let { b -> onSavePhoto(b.toJpeg()) } }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(340.dp).clip(MaterialTheme.shapes.large)) {
                RouteMap(workout.route, Modifier.fillMaxSize(), animate = true)
            }
        }
        item {
            SectionCard {
                if (justFinished) {
                    Text("Atividade concluída", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                }
                Text(workout.name, style = MaterialTheme.typography.headlineSmall)
                Text(workout.whenText(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Row {
                    Stat("Tempo", formatDuration(workout.durationMillis), Modifier.weight(1f))
                    Stat("Distância", formatDistance(workout.distanceMeters), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row {
                    Stat("Movimento", "${workout.intensity.fmt()} pts", Modifier.weight(1f))
                    Stat("Passos", if (workout.steps > 0) workout.steps.fmt() else "—", Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard {
                Text("Foto", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = "Foto da atividade",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(MaterialTheme.shapes.medium),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { camera.launch(null) }) { Text("Tirar outra") }
                        TextButton(onClick = onRemovePhoto) { Text("Remover", color = MaterialTheme.colorScheme.error) }
                    }
                } else {
                    Text(
                        "Registre o momento: uma selfie no fim da corrida, a vista do percurso, o grupo reunido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { camera.launch(null) }, shape = CircleShape, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Câmera")
                        }
                        FilledTonalButton(
                            onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Galeria")
                        }
                    }
                }
            }
        }
        if (justFinished) item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
                Text("Concluir", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

/** History of finished workouts. */
@Composable
fun WorkoutsScreen(
    workouts: List<Workout>,
    photoVersion: Int,
    loadPhoto: suspend (String) -> ByteArray?,
    contentPadding: PaddingValues,
    onOpen: (Workout) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (workouts.isEmpty()) item {
            SectionCard {
                Text("Nenhuma atividade ainda", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Toque em Iniciar atividade na aba Hoje. Quando parar, ela aparece aqui com o percurso no mapa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(workouts, key = { it.id }) { w ->
            val photo = rememberPhoto(w.id, photoVersion, loadPhoto)
            Card(
                onClick = { onOpen(w) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(96.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            photo != null -> Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            w.route.size >= 2 -> RouteCanvas(w.route, modifier = Modifier.fillMaxSize(), strokeWidthDp = 3f)
                            else -> Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(w.whenText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStat(formatDuration(w.durationMillis), "tempo")
                            MiniStat(formatDistance(w.distanceMeters), "distância")
                            MiniStat(w.intensity.fmt(), "pts")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
