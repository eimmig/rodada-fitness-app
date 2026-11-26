package br.edu.utfpr.rodada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import br.edu.utfpr.rodada.data.Rules
import br.edu.utfpr.rodada.ui.components.fmt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            content()
        }
    }
}

/** Explains each permission before asking for it (principle 4 in PRODUCT.md). */
@Composable
fun PermissionsSheet(onContinue: () -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Text("Antes de começar", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Reason(Icons.Rounded.Notifications, "Aviso fixo", "Mostra que o Rodada está medindo. É por ele que você para a atividade sem abrir o app.")
        Reason(Icons.AutoMirrored.Rounded.DirectionsWalk, "Atividade física", "Para contar passos. Sem ela, só o movimento do acelerômetro conta.")
        Reason(Icons.Rounded.LocationOn, "Localização", "Para desenhar seu percurso no mapa quando a atividade terminar.")
        Reason(Icons.Rounded.BatteryChargingFull, "Bateria", "O sensor fica ligado durante a atividade, inclusive com a tela apagada. Pare quando o dia acabar.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
            Text("Continuar", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Reason(icon: ImageVector, title: String, text: String) {
    Row(Modifier.padding(bottom = 16.dp)) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class GoalOption(val name: String, val points: Int, val steps: Int, val hint: String)

private val Goals = listOf(
    GoalOption("Leve", 1200, 4000, "≈ 20 min de caminhada"),
    GoalOption("Padrão", Rules.DEFAULT_INTENSITY_GOAL, Rules.DEFAULT_STEPS_GOAL, "≈ 30 min de caminhada"),
    GoalOption("Puxada", 2700, 9000, "≈ 45 min de caminhada"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupSheet(onCreate: (name: String, days: Int?, points: Int, steps: Int) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    // null = open-ended (no end date).
    var days by rememberSaveable { mutableStateOf<Int?>(Rules.DEFAULT_DURATION_DAYS) }
    var goal by rememberSaveable { mutableIntStateOf(1) }
    var sending by remember { mutableStateOf(false) }

    Sheet(onDismiss) {
        Text("Novo desafio", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nome do grupo") },
            placeholder = { Text("Turma PM46S") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Text("Duração", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 14, 30, null).forEach { d ->
                FilterChip(
                    selected = days == d,
                    onClick = { days = d },
                    label = { Text(if (d == null) "Sem fim" else "$d dias") },
                    shape = CircleShape,
                )
            }
        }
        if (days == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "O desafio não acaba: os dias conquistados vão somando enquanto o grupo existir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Meta do dia", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goals.forEachIndexed { i, g ->
                FilterChip(selected = goal == i, onClick = { goal = i }, label = { Text(g.name) }, shape = CircleShape)
            }
        }
        val g = Goals[goal]
        Spacer(Modifier.height(6.dp))
        Text(
            "${g.points.fmt()} pontos de movimento ou ${g.steps.fmt()} passos (${g.hint}).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { sending = true; onCreate(name, days, g.points, g.steps) },
            enabled = name.isNotBlank() && !sending,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
        ) {
            if (sending) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            else Text("Criar e gerar código", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun JoinWithCodeSheet(
    onJoin: (code: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val complete = code.length == Rules.CODE_LENGTH
    val submit = {
        sending = true
        error = null
        onJoin(code) { e -> error = e; sending = false }
    }

    Sheet(onDismiss) {
        Text("Entrar com código", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Peça o código de 6 letras para quem criou o grupo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = Rules.normalizeCode(it).take(Rules.CODE_LENGTH); error = null },
            label = { Text("Código") },
            singleLine = true,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.2.em),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (complete && !sending) submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Spacer(Modifier.weight(1f))
            Button(onClick = submit, enabled = complete && !sending, shape = CircleShape, modifier = Modifier.height(48.dp)) {
                if (sending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Entrar no grupo")
            }
        }
    }
}
