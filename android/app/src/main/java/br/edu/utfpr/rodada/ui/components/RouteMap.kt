package br.edu.utfpr.rodada.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.BuildConfig
import br.edu.utfpr.rodada.R
import br.edu.utfpr.rodada.data.RoutePoint
import br.edu.utfpr.rodada.ui.theme.LocalReduceMotion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.cos

/** Draw-on progress 0→1 for the route, played once when the screen opens. */
@Composable
private fun rememberRouteProgress(key: Any, animate: Boolean, start: Boolean): Float {
    val reduce = LocalReduceMotion.current
    val progress = remember(key) { Animatable(if (animate && !reduce) 0f else 1f) }
    LaunchedEffect(key, start) {
        if (start && progress.value < 1f) progress.animateTo(1f, tween(2600, easing = FastOutSlowInEasing))
    }
    return progress.value
}

/**
 * The finished route on a map, drawn on like a line being traced.
 * Falls back to a plain canvas when there is no Maps API key or the tiles never load.
 */
@Composable
fun RouteMap(route: List<RoutePoint>, modifier: Modifier = Modifier, animate: Boolean = true) {
    val description = Modifier.semantics { contentDescription = "Mapa do percurso com ${route.size} pontos" }
    if (route.size < 2) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            Text(
                "Sem percurso: o GPS estava desligado ou você ficou parado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }
    // onMapLoaded only fires once tiles arrive. If they never do (offline, or the key is not
    // authorized for this package) the map draws nothing at all, so fall back to the canvas.
    var tilesFailed by remember(route) { mutableStateOf(!BuildConfig.HAS_MAPS_KEY) }
    var mapLoaded by remember(route) { mutableStateOf(false) }
    LaunchedEffect(route) {
        kotlinx.coroutines.delay(2000)
        if (!mapLoaded) tilesFailed = true
    }
    if (tilesFailed) {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer).then(description)) {
            RouteCanvas(route, progress = rememberRouteProgress(route, animate, start = true), modifier = Modifier.fillMaxSize())
        }
        return
    }
    MapRoute(route, modifier.then(description), animate) { mapLoaded = true }
}

@Composable
private fun MapRoute(route: List<RoutePoint>, modifier: Modifier, animate: Boolean, onLoaded: () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val line = MaterialTheme.colorScheme.primary
    val points = remember(route) { route.map { LatLng(it.lat, it.lng) } }
    val bounds = remember(points) { LatLngBounds.builder().apply { points.forEach(::include) }.build() }
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(bounds.center, 15f) }
    var loaded by remember { mutableStateOf(false) }
    val progress = rememberRouteProgress(route, animate, start = loaded)

    val count = (2 + (points.size - 2) * progress).toInt().coerceIn(2, points.size)
    val visible = points.subList(0, count)

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, if (dark) R.raw.map_style_dark else R.raw.map_style_light),
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = true,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
        ),
        onMapLoaded = {
            camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            loaded = true
            onLoaded()
        },
    ) {
        // Soft halo under the line, then the line itself, then the moving head.
        Polyline(points = visible, color = line.copy(alpha = 0.25f), width = 28f, jointType = JointType.ROUND, startCap = RoundCap(), endCap = RoundCap())
        Polyline(points = visible, color = line, width = 12f, jointType = JointType.ROUND, startCap = RoundCap(), endCap = RoundCap())
        Circle(center = points.first(), radius = 6.0, fillColor = Color.White, strokeColor = line, strokeWidth = 6f)
        Circle(center = visible.last(), radius = 8.0, fillColor = line, strokeColor = Color.White, strokeWidth = 6f)
    }
}

/** Route projected into the box and drawn with a path. Used as map fallback and as list thumbnail. */
@Composable
fun RouteCanvas(route: List<RoutePoint>, progress: Float = 1f, modifier: Modifier = Modifier, strokeWidthDp: Float = 5f) {
    val line = MaterialTheme.colorScheme.primary
    val halo = line.copy(alpha = 0.22f)
    Canvas(modifier) {
        if (route.size < 2) return@Canvas
        // Equirectangular projection, good enough for a few kilometres.
        val lat0 = route.map { it.lat }.average()
        val k = cos(Math.toRadians(lat0))
        val xs = route.map { it.lng * k }
        val ys = route.map { -it.lat }
        val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
        val pad = size.minDimension * 0.14f
        val scale = minOf((size.width - 2 * pad) / (maxX - minX).coerceAtLeast(1e-9), (size.height - 2 * pad) / (maxY - minY).coerceAtLeast(1e-9))
        val offX = (size.width - (maxX - minX) * scale) / 2
        val offY = (size.height - (maxY - minY) * scale) / 2
        fun p(i: Int) = Offset(((xs[i] - minX) * scale + offX).toFloat(), ((ys[i] - minY) * scale + offY).toFloat())

        val full = Path().apply {
            moveTo(p(0).x, p(0).y)
            for (i in 1 until route.size) lineTo(p(i).x, p(i).y)
        }
        val measure = PathMeasure().apply { setPath(full, false) }
        val drawn = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), drawn, true)
        val w = strokeWidthDp.dp.toPx()
        drawPath(drawn, halo, style = Stroke(width = w * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(drawn, line, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color.White, radius = w * 0.9f, center = p(0))
        drawCircle(line, radius = w * 0.9f, center = p(0), style = Stroke(width = w * 0.5f))
        val head = measure.getPosition(measure.length * progress.coerceIn(0f, 1f))
        if (progress > 0f) {
            drawCircle(Color.White, radius = w * 1.3f, center = head)
            drawCircle(line, radius = w * 0.95f, center = head)
        }
    }
}
