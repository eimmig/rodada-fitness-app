package br.edu.utfpr.rodada.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.RankingRow
import br.edu.utfpr.rodada.data.StandingRow
import br.edu.utfpr.rodada.ui.theme.RodadaTheme
import kotlin.math.abs

@Composable
private fun medalColor(position: Int): Color? {
    val b = RodadaTheme.colors
    return when (position) { 1 -> b.gold; 2 -> b.silver; 3 -> b.bronze; else -> null }
}

@Composable
private fun RowShell(isYou: Boolean, description: String, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(if (isYou) RodadaTheme.colors.you else Color.Transparent, RoundedCornerShape(16.dp))
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

@Composable
private fun PositionNumber(position: Int) {
    val medal = medalColor(position)
    Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
        Text(
            "$position",
            style = MaterialTheme.typography.titleMedium,
            color = medal ?: MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** One member in a group's standings. */
@Composable
fun StandingItem(row: StandingRow, isYou: Boolean, modifier: Modifier = Modifier) {
    val brand = RodadaTheme.colors
    val description = buildString {
        append("${row.position}º, ${if (isYou) "você" else row.name}, ${row.roundsWon} dias, ${row.intensity.fmt()} pontos")
        if (row.wonToday) append(", conquistou hoje")
        if (row.movement > 0) append(", subiu ${row.movement}")
        if (row.movement < 0) append(", caiu ${-row.movement}")
    }
    RowShell(isYou, description, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PositionNumber(row.position)
            Spacer(Modifier.width(8.dp))
            Avatar(row.uid, row.name, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isYou && row.name != "Você") "${row.name} (você)" else row.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.wonToday) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = brand.achieved, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    "${row.intensity.fmt()} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Movement(row.movement)
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${row.roundsWon}", style = MaterialTheme.typography.headlineSmall)
                Text(if (row.roundsWon == 1) "dia" else "dias", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Movement(value: Int) {
    if (value == 0) return
    val brand = RodadaTheme.colors
    val up = value > 0
    val color = if (up) brand.achieved else MaterialTheme.colorScheme.error
    Row(
        Modifier.background(color.copy(alpha = 0.12f), CircleShape).padding(start = 2.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (up) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text("${abs(value)}", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** One person in the app-wide individual ranking. */
@Composable
fun RankingItem(row: RankingRow, isYou: Boolean, modifier: Modifier = Modifier) {
    RowShell(
        isYou,
        "${row.position}º, ${if (isYou) "você" else row.name}, ${row.intensity.fmt()} pontos, ${row.steps.fmt()} passos",
        modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PositionNumber(row.position)
            Spacer(Modifier.width(8.dp))
            Avatar(row.uid, row.name, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${row.steps.fmt()} passos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.intensity.fmt(), style = MaterialTheme.typography.titleLarge)
                Text("pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Top three on rounded steps: 2nd, 1st, 3rd. */
@Composable
fun Podium(top: List<StandingRow>, youUid: String, modifier: Modifier = Modifier) {
    if (top.isEmpty()) return
    val order = listOfNotNull(top.getOrNull(1), top.getOrNull(0), top.getOrNull(2))
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        order.forEach { row ->
            val first = row.position == 1
            val stepHeight = when (row.position) { 1 -> 96.dp; 2 -> 72.dp; else -> 56.dp }
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "${row.position}º lugar, ${if (row.uid == youUid) "você" else row.name}, ${row.roundsWon} dias"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(row.uid, row.name, if (first) 72.dp else 56.dp, ring = medalColor(row.position))
                Spacer(Modifier.height(8.dp))
                Text(
                    row.name.substringBefore(' '),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${row.roundsWon} dias", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(stepHeight)
                        .background(
                            if (row.uid == youUid) RodadaTheme.colors.you else MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${row.position}", style = MaterialTheme.typography.headlineLarge, color = medalColor(row.position) ?: MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
