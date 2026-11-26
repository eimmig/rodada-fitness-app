package br.edu.utfpr.rodada.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.StandingRow
import br.edu.utfpr.rodada.sensor.Monitor
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.rounded.ArrowDropUp
import br.edu.utfpr.rodada.ui.theme.LocalReduceMotion
import br.edu.utfpr.rodada.ui.theme.RodadaTheme
import kotlinx.coroutines.delay

/**
 * Main card on the Today tab: today's movement against the goal, steps,
 * live meter while active, and the start/stop button.
 */
@Composable
fun ActivityCard(
    intensity: Int,
    intensityGoal: Int,
    steps: Int,
    stepsGoal: Int,
    achieved: Boolean,
    monitor: Monitor,
    groupRank: StandingRow? = null,
    onToggle: () -> Unit,
) {
    val brand = RodadaTheme.colors
    val reduce = LocalReduceMotion.current
    val shownIntensity by animateIntAsState(
        intensity,
        animationSpec = if (reduce) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessVeryLow),
        label = "intensity",
    )

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (achieved) "Dia conquistado" else "Movimento de hoje",
                style = MaterialTheme.typography.titleMedium,
                color = if (achieved) brand.achieved else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (monitor.active) LiveBadge()
            else if (groupRank != null) RankChip(groupRank)
        }
        if (monitor.active && groupRank != null) {
            Spacer(Modifier.height(6.dp))
            RankChip(groupRank)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Movimento hoje: ${intensity.fmt()} de ${intensityGoal.fmt()} pontos"
            },
        ) {
            Text(shownIntensity.fmt(), style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                "/ ${intensityGoal.fmt()} pts",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        GoalBar(progress = intensity.toFloat() / intensityGoal, achieved = achieved)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${steps.fmt()} de ${stepsGoal.fmt()} passos" + if (!monitor.stepsAvailable) " · sem sensor de passos aqui" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!achieved) {
                val left = (intensityGoal - intensity).coerceAtLeast(0)
                Text("faltam ${left.fmt()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (monitor.active) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveMeter(monitor.currentLevel, Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    ElapsedTime(monitor.startedAtMillis)
                    Text(
                        if (monitor.tracking) formatKm(monitor.distanceMeters) else "sem GPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        val container by animateColorAsState(
            if (monitor.active) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
            label = "buttonColor",
        )
        Button(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = if (monitor.active) brand.live else MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            AnimatedContent(monitor.active, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "buttonLabel") { active ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (active) "Parar atividade" else "Iniciar atividade", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Rounded progress bar towards the daily goal, filled with a spring. */
@Composable
fun GoalBar(progress: Float, achieved: Boolean, modifier: Modifier = Modifier) {
    val brand = RodadaTheme.colors
    val reduce = LocalReduceMotion.current
    val fraction by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = if (reduce) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "goal",
    )
    val color by animateColorAsState(if (achieved) brand.achieved else MaterialTheme.colorScheme.primary, label = "goalColor")
    Box(
        modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(brand.track, CircleShape)
            .semantics { contentDescription = "Meta do dia: ${(progress.coerceIn(0f, 1f) * 100).toInt()}%" },
    ) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction.coerceAtLeast(0.04f)).fillMaxHeight().background(color, CircleShape))
        }
    }
}

/** Twelve rounded bars that breathe with the last second's movement level. */
@Composable
fun LiveMeter(level: Int, modifier: Modifier = Modifier) {
    val brand = RodadaTheme.colors
    Row(
        modifier.height(32.dp).semantics { contentDescription = "Movimento agora: nível $level de 10" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(12) { i ->
            val lit = i < (level * 12 / 10f)
            val target = if (lit) (10 + i * 2).dp else 6.dp
            val h by animateDpAsState(target, spring(stiffness = Spring.StiffnessMediumLow), label = "bar$i")
            Box(
                Modifier
                    .weight(1f)
                    .height(h)
                    .background(if (lit) MaterialTheme.colorScheme.primary else brand.track, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun ElapsedTime(startedAt: Long) {
    val text by produceState("00:00", startedAt) {
        while (true) {
            val s = ((System.currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
            value = if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
            delay(1000)
        }
    }
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
}

private fun formatKm(meters: Double): String =
    if (meters >= 1000) String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f km", meters / 1000) else "${meters.toInt()} m"

/** "3º no grupo", with a green climb arrow when today's activity moved you up. */
@Composable
private fun RankChip(row: StandingRow) {
    val brand = RodadaTheme.colors
    val up = row.movement > 0
    Row(
        Modifier
            .background(if (up) brand.achievedContainer else MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(start = if (up) 4.dp else 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (up) Icon(Icons.Rounded.ArrowDropUp, contentDescription = "subiu", tint = brand.achieved, modifier = Modifier.size(20.dp))
        Text(
            "${row.position}º no grupo",
            style = MaterialTheme.typography.labelMedium,
            color = if (up) brand.achieved else MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
