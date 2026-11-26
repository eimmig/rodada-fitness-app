package br.edu.utfpr.rodada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.RankingRow
import br.edu.utfpr.rodada.ui.components.RankingItem
import br.edu.utfpr.rodada.ui.components.SectionCard

/** Individual ranking of everyone who uses the app. */
@Composable
fun RankingScreen(
    today: List<RankingRow>,
    total: List<RankingRow>,
    youUid: String,
    contentPadding: PaddingValues,
) {
    var showTotal by rememberSaveable { mutableStateOf(false) }
    val rows = if (showTotal) total else today
    val card = MaterialTheme.colorScheme.surfaceContainerLowest
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            val colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                SegmentedButton(
                    selected = !showTotal,
                    onClick = { showTotal = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2, CircleShape),
                    colors = colors,
                ) { Text("Hoje") }
                SegmentedButton(
                    selected = showTotal,
                    onClick = { showTotal = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2, CircleShape),
                    colors = colors,
                ) { Text("Acumulado") }
            }
        }
        if (rows.isEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SectionCard {
                        Text(
                            if (showTotal) "Ninguém pontuou ainda." else "Ninguém pontuou hoje ainda. Inicie uma atividade e saia na frente.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        itemsIndexed(rows, key = { _, r -> r.uid }) { i, row ->
            RankingItem(
                row, isYou = row.uid == youUid,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(card, segmentShape(i, rows.size))
                    .padding(8.dp),
            )
        }
    }
}
