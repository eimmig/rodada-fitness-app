package br.edu.utfpr.rodada.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.Group
import br.edu.utfpr.rodada.data.Rules
import br.edu.utfpr.rodada.ui.HomeUiState
import br.edu.utfpr.rodada.ui.components.ActivityCard
import br.edu.utfpr.rodada.ui.components.Avatar
import br.edu.utfpr.rodada.ui.components.DaysStrip
import br.edu.utfpr.rodada.ui.components.SectionCard
import br.edu.utfpr.rodada.ui.components.StandingItem
import br.edu.utfpr.rodada.ui.components.StreakChip
import br.edu.utfpr.rodada.ui.theme.LocalReduceMotion
import java.time.LocalDate

/** Rounded corners only on the first and last item, so consecutive rows read as one card. */
fun segmentShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 24.dp else 0.dp
    val bottom = if (index == count - 1) 24.dp else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun TodayScreen(
    ui: HomeUiState,
    contentPadding: PaddingValues,
    onToggleActivity: () -> Unit,
    onFocusGroup: (String) -> Unit,
    onOpenGroup: (Group) -> Unit,
    onCreateGroup: () -> Unit,
    onJoinWithCode: () -> Unit,
    onSignOut: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val group = ui.group
    val card = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "header") {
            Header(ui, onFocusGroup, onSignOut)
        }
        if (ui.isDemo) item(key = "demo") { DemoNotice() }
        item(key = "activity") {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ActivityCard(
                    intensity = ui.intensityToday,
                    intensityGoal = ui.intensityGoal,
                    steps = ui.stepsToday,
                    stepsGoal = ui.stepsGoal,
                    achieved = ui.achievedToday,
                    monitor = ui.monitor,
                    groupRank = ui.standings.firstOrNull { it.uid == ui.user.uid },
                    onToggle = onToggleActivity,
                )
            }
        }
        if (group == null) {
            if (!ui.loading) item(key = "empty") {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { NoGroup(onCreateGroup, onJoinWithCode) }
            }
        } else {
            item(key = "days") {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { DaysStrip(group, ui.myDays, ui.achievedToday) }
            }
            item(key = "rankingTitle") {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        .fillMaxWidth()
                        .background(card, segmentShape(0, 2))
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ranking do grupo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onOpenGroup(group) }) { Text("Ver grupo") }
                }
            }
            // Preview: podium plus you. The full table lives in "Ver grupo".
            val you = ui.standings.firstOrNull { it.uid == ui.user.uid }
            val preview = (ui.standings.take(3) + listOfNotNull(you)).distinctBy { it.uid }
            itemsIndexed(preview, key = { _, row -> row.uid }) { i, row ->
                StandingItem(
                    row = row,
                    isYou = row.uid == ui.user.uid,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = if (reduceMotion) null
                            else spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                        )
                        .padding(horizontal = 16.dp)
                        .background(card, segmentShape(i + 1, preview.size + 1))
                        .padding(horizontal = 8.dp)
                        .padding(bottom = if (i == preview.lastIndex) 8.dp else 0.dp),
                )
            }
        }
    }
    // Scrim so scrolled content never sits under the status bar icons.
    Spacer(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.surface)
    )
    }
}

@Composable
private fun Header(ui: HomeUiState, onFocusGroup: (String) -> Unit, onSignOut: () -> Unit) {
    val firstName = ui.user.name.substringBefore(' ')
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (firstName == "Você") "Bora se mexer?" else "Olá, $firstName",
                style = MaterialTheme.typography.headlineMedium,
            )
            GroupSelector(ui.group, ui.groups, onFocusGroup)
        }
        StreakChip(ui.streak)
        Spacer(Modifier.width(4.dp))
        AccountMenu(ui.user.uid, ui.user.name, onSignOut)
    }
}

@Composable
private fun GroupSelector(group: Group?, groups: List<Group>, onFocus: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val subtitle = group?.let { "${it.name} · ${Rules.dayLabel(it, LocalDate.now())}" } ?: "Sem grupo ainda"
    Box {
        Row(
            Modifier
                .padding(top = 2.dp)
                .then(
                    if (groups.size > 1) Modifier.clickable(role = Role.DropdownList, onClickLabel = "Trocar de grupo") { open = true }
                    else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (groups.size > 1) Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            groups.forEach { g ->
                DropdownMenuItem(
                    text = { Text(g.name) },
                    onClick = { open = false; onFocus(g.id) },
                    trailingIcon = { if (g.id == group?.id) Icon(Icons.Rounded.Check, contentDescription = "Em foco") },
                )
            }
        }
    }
}

@Composable
private fun AccountMenu(uid: String, name: String, onSignOut: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = "Conta de $name" }) {
            Avatar(uid, name, 36.dp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            DropdownMenuItem(
                text = { Text("Sair da conta") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                onClick = { open = false; onSignOut() },
            )
        }
    }
}

@Composable
private fun DemoNotice() {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "Modo demonstração · dados fictícios",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun NoGroup(onCreateGroup: () -> Unit, onJoinWithCode: () -> Unit) {
    SectionCard {
        Text("Desafie sua turma", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Seu movimento de hoje já está contando. Crie um grupo e passe o código, ou entre com o código de alguém.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCreateGroup, shape = CircleShape) { Text("Criar grupo") }
            OutlinedButton(onClick = onJoinWithCode, shape = CircleShape) { Text("Entrar com código") }
        }
    }
}
