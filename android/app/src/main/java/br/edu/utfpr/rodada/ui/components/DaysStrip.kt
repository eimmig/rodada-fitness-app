package br.edu.utfpr.rodada.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.DailyActivity
import br.edu.utfpr.rodada.data.Group
import br.edu.utfpr.rodada.data.Rules
import br.edu.utfpr.rodada.ui.theme.RodadaTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class DayState { ACHIEVED, MISSED, TODAY, TODAY_ACHIEVED, FUTURE }

/** One dot per day of the challenge: achieved days light up, like check-ins. */
@Composable
fun DaysStrip(group: Group, myDays: List<DailyActivity>, achievedToday: Boolean) {
    val today = LocalDate.now()
    val achievedSet = myDays.filter { Rules.counts(it, group.intensityGoal, group.stepsGoal) }.map { it.day }.toSet()
    // Fixed period: every day of it. Open-ended: from the start until one week ahead.
    val lastDay = group.endDate ?: today.plusDays(6)
    val days = generateSequence(group.startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }.toList()
    val won = achievedSet.count { !it.isBefore(group.startDate) && !it.isAfter(today) && it != today } + if (achievedToday) 1 else 0
    val round = Rules.currentRound(group, today)
    val listState = rememberLazyListState()
    LaunchedEffect(group.id) { listState.scrollToItem((round - 4).coerceAtLeast(0)) }

    SectionCard(padding = PaddingValues(vertical = 20.dp)) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Desafio", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "$won de $round ${if (round == 1) "dia" else "dias"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(days, key = { it.toEpochDay() }) { day ->
                val state = when {
                    day == today && achievedToday -> DayState.TODAY_ACHIEVED
                    day == today -> DayState.TODAY
                    day.isAfter(today) -> DayState.FUTURE
                    day in achievedSet -> DayState.ACHIEVED
                    else -> DayState.MISSED
                }
                DayDot(day, state)
            }
        }
    }
}

@Composable
private fun DayDot(day: LocalDate, state: DayState) {
    val brand = RodadaTheme.colors
    val c = MaterialTheme.colorScheme
    val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR")).take(3).replaceFirstChar { it.uppercase() }
    val description = when (state) {
        DayState.ACHIEVED, DayState.TODAY_ACHIEVED -> "conquistado"
        DayState.MISSED -> "não conquistado"
        DayState.TODAY -> "hoje, em andamento"
        DayState.FUTURE -> "ainda não chegou"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Dia ${day.dayOfMonth}, $description" },
    ) {
        Text(
            weekday,
            style = MaterialTheme.typography.labelSmall,
            color = if (state == DayState.TODAY || state == DayState.TODAY_ACHIEVED) c.primary else c.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        val base = Modifier.size(40.dp)
        val dot = when (state) {
            DayState.ACHIEVED, DayState.TODAY_ACHIEVED -> base.background(brand.achieved, CircleShape)
            DayState.TODAY -> base.border(2.dp, c.primary, CircleShape)
            DayState.MISSED -> base.background(brand.track, CircleShape)
            DayState.FUTURE -> base.border(1.dp, c.outlineVariant, CircleShape)
        }
        Box(dot, contentAlignment = Alignment.Center) {
            when (state) {
                DayState.ACHIEVED, DayState.TODAY_ACHIEVED ->
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onTertiary, modifier = Modifier.size(22.dp))
                else -> Text(
                    "${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleSmall,
                    color = when (state) {
                        DayState.TODAY -> c.primary
                        DayState.FUTURE -> c.outline
                        else -> c.onSurfaceVariant
                    },
                )
            }
        }
    }
}
