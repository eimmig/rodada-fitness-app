package br.edu.utfpr.rodada.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.edu.utfpr.rodada.ui.theme.LocalReduceMotion
import br.edu.utfpr.rodada.ui.theme.RodadaTheme
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/*
 * The Rodada mark: a tapered arrow turning around a clock ("one more day around").
 * Same geometry as the launcher icon (generated from these numbers), in units of a
 * 108-unit adaptive icon. Angles are degrees clockwise from 12 o'clock.
 */
private const val R = 23f
private const val W_MIN = 1.6f
private const val W_MAX = 8f
private const val HEAD_HALF = 7.6f
private const val TAIL = 40f
private const val HEAD = 318f
private const val TIP = 24f
private const val FACE = 13f

/**
 * @param spinning keeps the arrow turning (used as the sign-in progress indicator).
 * @param drawOn plays the arrow drawing itself around the clock when it first appears.
 * @param bold heavier strokes for small sizes, where it has to match a bold letter.
 */
@Composable
fun RodadaMark(
    modifier: Modifier = Modifier,
    spinning: Boolean = false,
    drawOn: Boolean = true,
    arrowColor: Color = Color.White,
    faceColor: Color = Color.White,
    handColor: Color = Color(0xFF5B3DF5),
    accentColor: Color = RodadaTheme.colors.streak,
    bold: Boolean = false,
) {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (drawOn && !reduce) 0f else 1f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    // Spin while busy; when it stops, settle forward on the nearest full turn.
    val turn = remember { Animatable(0f) }
    LaunchedEffect(spinning, reduce) {
        if (reduce) return@LaunchedEffect
        if (spinning) {
            while (true) turn.animateTo(turn.value + 360f, tween(900, easing = LinearEasing))
        } else {
            val target = kotlin.math.ceil(turn.value / 360f) * 360f
            turn.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    // The orange second hand never stops: the clock is always running.
    val ticking by rememberInfiniteTransition(label = "second-hand")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "second")
    val seconds = if (reduce) 0f else ticking

    Canvas(modifier.semantics { contentDescription = "Logo do Rodada" }) {
        val headHalf = if (bold) 9f else HEAD_HALF
        val s = size.minDimension / 2f / (R + headHalf)
        val c = center
        // Entrance: the arrow grows around and swings into place.
        rotate(turn.value + (1f - progress.value) * -140f, c) {
            if (bold) drawArrow(c, s, progress.value, arrowColor, wMin = 6f, wMax = 11f, headHalf = headHalf)
            else drawArrow(c, s, progress.value, arrowColor)
        }
        drawCircle(faceColor, radius = (if (bold) 12.5f else FACE) * s, center = c)
        if (bold) {
            // Too small for a moving second hand: just the 10:10 hands and the hub.
            drawHand(c, s, 305f, 6.4f, 4.2f, handColor)
            drawHand(c, s, 60f, 8.6f, 3.6f, handColor)
            drawCircle(accentColor, radius = 3f * s, center = c)
        } else {
            drawHand(c, s, 305f, 6.6f, 2.8f, handColor)
            drawHand(c, s, 60f, 9.6f, 2.4f, handColor)
            drawHand(c, s, seconds + 180f, 10.2f, 1.1f, accentColor, tail = 2.6f)
            drawCircle(accentColor, radius = 2.2f * s, center = c)
        }
    }
}

private fun point(c: Offset, s: Float, theta: Float, r: Float): Offset {
    val a = Math.toRadians(theta.toDouble())
    return Offset(c.x + s * r * sin(a).toFloat(), c.y - s * r * cos(a).toFloat())
}

/** Tapered shaft from the tail to [progress] of the sweep, with the head always at the front. */
private fun DrawScope.drawArrow(
    c: Offset, s: Float, progress: Float, color: Color,
    wMin: Float = W_MIN, wMax: Float = W_MAX, headHalf: Float = HEAD_HALF,
) {
    if (progress <= 0.02f) return
    val head = TAIL + (HEAD - TAIL) * progress
    val n = 72
    val outer = ArrayList<Offset>(n + 1)
    val inner = ArrayList<Offset>(n + 1)
    for (i in 0..n) {
        val t = i / n.toFloat()
        val theta = TAIL + (head - TAIL) * t
        val w = wMin + (wMax - wMin) * t.pow(0.8f)
        outer += point(c, s, theta, R + w / 2)
        inner += point(c, s, theta, R - w / 2)
    }
    val path = Path().apply {
        moveTo(outer[0].x, outer[0].y)
        outer.drop(1).forEach { lineTo(it.x, it.y) }
        point(c, s, head, R + headHalf).let { lineTo(it.x, it.y) }
        point(c, s, head + TIP, R).let { lineTo(it.x, it.y) }
        point(c, s, head, R - headHalf).let { lineTo(it.x, it.y) }
        inner.asReversed().forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, color)
    drawCircle(color, radius = s * wMin / 2, center = point(c, s, TAIL, R))
}

private fun DrawScope.drawHand(c: Offset, s: Float, theta: Float, length: Float, width: Float, color: Color, tail: Float = 0f) {
    drawLine(
        color,
        start = point(c, s, theta + 180f, tail),
        end = point(c, s, theta, length),
        strokeWidth = width * s,
        cap = StrokeCap.Round,
    )
}

/**
 * "Rodada" with the mark standing in for the "o". The placeholder sits on the baseline and is
 * slightly taller than the x-height, like a round letter's overshoot.
 */
@Composable
fun RodadaWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Color.White,
    handColor: Color = Color(0xFF5B3DF5),
    spinning: Boolean = false,
) {
    val text = buildAnnotatedString {
        append("R")
        appendInlineContent("mark", "o")
        append("dada")
    }
    val inline = mapOf(
        "mark" to InlineTextContent(Placeholder(0.7.em, 0.7.em, PlaceholderVerticalAlign.AboveBaseline)) {
            RodadaMark(
                Modifier
                    .fillMaxSize()
                    // The ring's lowest edge sits a little inside the box; drop it onto the baseline.
                    .graphicsLayer { translationY = size.height * 0.04f },
                spinning = spinning,
                drawOn = false,
                bold = true,
                arrowColor = color,
                faceColor = color,
                handColor = handColor,
            )
        },
    )
    Text(
        text,
        inlineContent = inline,
        style = style,
        color = color,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Rodada" },
    )
}
