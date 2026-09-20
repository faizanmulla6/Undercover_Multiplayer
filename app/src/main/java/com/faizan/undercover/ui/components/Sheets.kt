package com.faizan.undercover.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faizan.undercover.GameViewModel
import com.faizan.undercover.model.HistoryEntry
import com.faizan.undercover.ui.theme.StencilLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("How to play", style = MaterialTheme.typography.headlineSmall)

            RuleBlock(
                "Roles",
                listOf(
                    "Civilian — the majority. Every civilian shares the same secret word.",
                    "Undercover — gets a different but closely related word, and has to blend in.",
                    "Blank — optional. No word at all; bluff from what everyone else says."
                )
            )
            RuleBlock(
                "A round",
                listOf(
                    "Add every agent and choose how many are undercover.",
                    "Pass the phone around — each agent holds the card to read their orders privately.",
                    "In the speaking order shown, everyone gives one short clue about their word.",
                    "Discuss, then vote out whoever the group suspects."
                )
            )
            RuleBlock(
                "Winning",
                listOf(
                    "Civilians win once every undercover and blank agent is out.",
                    "Undercover wins the moment they equal or outnumber the remaining civilians.",
                    "A blank agent who is voted out may guess the civilian word — a correct guess steals the round."
                )
            )
            RuleBlock(
                "Scoring",
                listOf(
                    "Civilians win → 2 points to each surviving civilian.",
                    "Undercover wins → 3 points to each surviving undercover or blank agent.",
                    "Blank agent guesses right → 4 points to them.",
                    "Points build up across rounds. Reset them from the scoreboard."
                )
            )
            RuleBlock(
                "Word bank",
                listOf(
                    "Built-in pairs are grouped in categories you can switch on and off.",
                    "Add your own pairs, or paste a whole list with bulk import.",
                    "Choose built-in, a mix, or custom-only for any night."
                )
            )
        }
    }
}

@Composable
private fun RuleBlock(title: String, lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            style = StencilLabel,
            color = MaterialTheme.colorScheme.primary
        )
        lines.forEach {
            Text("•  $it", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardSheet(vm: GameViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmReset by remember { mutableStateOf(false) }
    val board = vm.leaderboard()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Scoreboard", style = MaterialTheme.typography.headlineSmall)

            if (board.isEmpty()) {
                Text(
                    "No agents yet — add some on the setup screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                board.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "#${index + 1}",
                                style = StencilLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(entry.first, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            entry.second.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    HorizontalDivider()
                }
            }

            if (vm.state.history.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "ROUND LOG",
                    style = StencilLabel,
                    color = MaterialTheme.colorScheme.primary
                )
                vm.state.history.reversed().forEach { HistoryRow(it) }
            }

            Spacer(Modifier.height(8.dp))
            if (!confirmReset) {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset scores") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { confirmReset = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    TextButton(
                        onClick = { vm.resetScores(); confirmReset = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Yes, reset") }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Round ${entry.roundNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(
                entry.title,
                style = StencilLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${entry.civilianWord} / ${entry.undercoverWord}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
