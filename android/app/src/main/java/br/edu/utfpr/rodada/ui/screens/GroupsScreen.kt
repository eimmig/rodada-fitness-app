package br.edu.utfpr.rodada.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import br.edu.utfpr.rodada.data.Group
import br.edu.utfpr.rodada.data.Rules
import br.edu.utfpr.rodada.data.StandingRow
import br.edu.utfpr.rodada.ui.components.Avatar
import br.edu.utfpr.rodada.ui.components.Podium
import br.edu.utfpr.rodada.ui.components.SectionCard
import br.edu.utfpr.rodada.ui.components.StandingItem
import br.edu.utfpr.rodada.ui.components.fmt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The user's groups. */
@Composable
fun GroupsScreen(
    groups: List<Group>,
    loading: Boolean,
    contentPadding: PaddingValues,
    onOpen: (Group) -> Unit,
    onJoinWithCode: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (groups.isEmpty() && !loading) {
            item {
                SectionCard {
                    Text("Nenhum grupo ainda", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Um grupo é um desafio com período e meta diária. Crie o seu no botão abaixo ou entre com o código que alguém passou.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onJoinWithCode, shape = CircleShape) { Text("Entrar com código") }
                }
            }
        }
        items(groups, key = { it.id }) { GroupCard(it, onClick = { onOpen(it) }) }
    }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    val today = LocalDate.now()
    val duration = group.durationDays
    val progress = duration?.let { Rules.currentRound(group, today).toFloat() / it }
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                Rules.dayLabel(group, today).replaceFirstChar { it.uppercase() } + if (duration == null) " · sem data para acabar" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarStack(group.members.entries.map { it.key to it.value })
                Spacer(Modifier.weight(1f))
                Text(
                    "${group.members.size} ${if (group.members.size == 1) "membro" else "membros"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress != null) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).height(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun AvatarStack(members: List<Pair<String, String>>) {
    val shown = members.take(5)
    val extra = members.size - shown.size
    Row {
        shown.forEachIndexed { i, (uid, name) ->
            Avatar(
                uid, name, 32.dp,
                modifier = Modifier
                    .offset(x = (-10 * i).dp)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
            )
        }
        if (extra > 0) {
            Box(
                Modifier.offset(x = (-10 * shown.size).dp).size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("+$extra", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pt-BR"))

/** A group's full standings: podium, invite code and the rest of the table. */
@Composable
fun GroupDetailScreen(group: Group, standings: List<StandingRow>, youUid: String, contentPadding: PaddingValues) {
    val card = MaterialTheme.colorScheme.surfaceContainerLowest
    val rest = standings.drop(3)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        if (standings.isNotEmpty()) item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SectionCard { Podium(standings.take(3), youUid) }
            }
        }
        if (rest.isNotEmpty()) itemsIndexed(rest, key = { _, r -> r.uid }) { i, row ->
            StandingItem(
                row, isYou = row.uid == youUid,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(card, segmentShape(i, rest.size))
                    .padding(8.dp),
            )
        }
        item { Box(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) { InviteCard(group) } }
    }
}

@Composable
private fun InviteCard(group: Group) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val invite = "Bora pro desafio \"${group.name}\" no Rodada! Código: ${group.code}"
    SectionCard {
        Text("Convide a turma", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                group.code,
                style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 0.18.em),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { contentDescription = "Código ${group.code.toList().joinToString(" ")}" },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { clipboard.setText(AnnotatedString(group.code)) },
                shape = CircleShape,
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copiar")
            }
            FilledTonalButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, invite)
                    context.startActivity(Intent.createChooser(send, "Convidar para ${group.name}"))
                },
                shape = CircleShape,
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar")
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Fact(
                "Período",
                group.endDate?.let { "${group.startDate.format(dateFormat)} – ${it.format(dateFormat)}" }
                    ?: "desde ${group.startDate.format(dateFormat)}",
            )
            Fact("Meta do dia", "${group.intensityGoal.fmt()} pts")
            Fact("ou", "${group.stepsGoal.fmt()} passos")
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
