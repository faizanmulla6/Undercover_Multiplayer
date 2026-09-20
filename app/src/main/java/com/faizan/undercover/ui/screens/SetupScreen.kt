package com.faizan.undercover.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.faizan.undercover.GameViewModel
import com.faizan.undercover.data.WordBank
import com.faizan.undercover.model.VoteStyle
import com.faizan.undercover.model.WordMode
import com.faizan.undercover.ui.components.SectionCard
import com.faizan.undercover.ui.components.SectionLabel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    vm: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state = vm.state
    val focus = LocalFocusManager.current

    var nameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var pairA by remember { mutableStateOf("") }
    var pairB by remember { mutableStateOf("") }
    var pairError by remember { mutableStateOf<String?>(null) }
    var showBulk by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Gather everyone around one phone. Add the agents, decide how many " +
                "are working undercover, then pass the device around for private orders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---------------- Agents ----------------
        SectionCard(title = "Agents on mission") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it.take(GameViewModel.NAME_LIMIT)
                        nameError = null
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Agent name") },
                    isError = nameError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        nameError = vm.addPlayer(nameInput)
                        if (nameError == null) nameInput = ""
                    })
                )
                FilledTonalIconButton(
                    onClick = {
                        nameError = vm.addPlayer(nameInput)
                        if (nameError == null) nameInput = ""
                    },
                    modifier = Modifier.height(56.dp).width(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add agent")
                }
            }

            nameError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (state.players.isEmpty()) {
                Text(
                    "No agents added yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.players.forEachIndexed { index, name ->
                        InputChip(
                            selected = false,
                            onClick = { vm.removePlayer(index) },
                            label = { Text(name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove $name",
                                    modifier = Modifier.height(16.dp)
                                )
                            }
                        )
                    }
                }
                val blanks = if (state.useBlank) 1 else 0
                val civilians = state.players.size - state.undercoverCount - blanks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${state.players.size} agents · $civilians civilians",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showClearConfirm = true }) { Text("Clear all") }
                }
            }
        }

        // ---------------- Mission parameters ----------------
        SectionCard(title = "Mission parameters") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Undercover agents", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "They get the decoy word",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { vm.setUndercoverCount(state.undercoverCount - 1) },
                        enabled = state.undercoverCount > 1
                    ) { Icon(Icons.Default.Remove, contentDescription = "Fewer undercover agents") }
                    Text(
                        state.undercoverCount.toString(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    FilledTonalIconButton(
                        onClick = { vm.setUndercoverCount(state.undercoverCount + 1) },
                        enabled = state.undercoverCount < state.maxUndercover
                    ) { Icon(Icons.Default.Add, contentDescription = "More undercover agents") }
                    
                    IconButton(onClick = { vm.autoBalance() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Suggest balanced count")
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Blank agent", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "One player gets no word and must bluff. If voted out, they get one " +
                            "guess at the civilian word to steal the round.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = state.useBlank,
                    onCheckedChange = { vm.setUseBlank(it) }
                )
            }

            HorizontalDivider()

            Text("Discussion timer", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 30, 60, 90, 120).forEach { seconds ->
                    FilterChip(
                        selected = state.timerSeconds == seconds,
                        onClick = { vm.setTimerSeconds(seconds) },
                        label = { Text(if (seconds == 0) "Off" else "${seconds}s") }
                    )
                }
            }

            Text("Voting style", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.voteStyle == VoteStyle.QUICK,
                    onClick = { vm.setVoteStyle(VoteStyle.QUICK) },
                    label = { Text("Quick tap") }
                )
                FilterChip(
                    selected = state.voteStyle == VoteStyle.TALLY,
                    onClick = { vm.setVoteStyle(VoteStyle.TALLY) },
                    label = { Text("Count votes") }
                )
            }
            Text(
                if (state.voteStyle == VoteStyle.QUICK) {
                    "Tap a name and confirm to eliminate them."
                } else {
                    "Tap a name once per vote, then eliminate whoever leads."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------------- Word bank ----------------
        SectionCard(title = "Word bank") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WordMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.wordMode == mode,
                        onClick = { vm.setWordMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    WordMode.BUILTIN -> "Built-in"
                                    WordMode.MIX -> "Mix"
                                    WordMode.CUSTOM -> "Custom only"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
            Text(
                "${vm.wordPool().size} word pairs in play.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.wordMode != WordMode.CUSTOM) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Categories")
                    TextButton(onClick = { showCategories = !showCategories }) {
                        Icon(
                            if (showCategories) Icons.Default.Remove else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (showCategories) "Hide" else "Show")
                    }
                }
                if (showCategories) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WordBank.categories.forEach { category ->
                            FilterChip(
                                selected = category !in state.disabledCategories,
                                onClick = { vm.toggleCategory(category) },
                                label = { Text("$category (${WordBank.countIn(category)})") }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            SectionLabel("Your own pairs")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pairA,
                    onValueChange = { pairA = it.take(GameViewModel.WORD_LIMIT); pairError = null },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Civilian word") }
                )
                OutlinedTextField(
                    value = pairB,
                    onValueChange = { pairB = it.take(GameViewModel.WORD_LIMIT); pairError = null },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Decoy word") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        pairError = vm.addCustomPair(pairA, pairB)
                        if (pairError == null) { pairA = ""; pairB = ""; focus.clearFocus() }
                    })
                )
            }
            pairError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        pairError = vm.addCustomPair(pairA, pairB)
                        if (pairError == null) { pairA = ""; pairB = "" }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Add pair") }
                OutlinedButton(
                    onClick = { showBulk = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ImportExport, contentDescription = null, Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bulk")
                }
            }

            if (state.customPairs.isEmpty()) {
                Text(
                    "No custom pairs yet — handy for inside jokes or a theme night.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val blocker = vm.setupBlocker()
        if (blocker != null) {
            Text(
                blocker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = { focus.clearFocus(); vm.startRound() },
            enabled = vm.canStart(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors()
        ) {
            Text(
                if (state.roundNumber == 0) "Begin briefing" else "Deal a new round",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear the agent list?") },
            text = { Text("Names are removed from this device. Scores stay on the leaderboard.") },
            confirmButton = {
                TextButton(onClick = { vm.clearPlayers(); showClearConfirm = false }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showBulk) {
        BulkPairsDialog(vm = vm, onDismiss = { showBulk = false })
    }
}

@Composable
private fun BulkPairsDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    val state = vm.state
    var text by remember { mutableStateOf(vm.exportPairs()) }
    var status by remember { mutableStateOf<String?>(null) }
    var showUrlEdit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk pairs") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Cloud sync",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Button(
                    onClick = { vm.syncFromUrl { msg -> status = msg } },
                    enabled = !state.syncing,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.ImportExport, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.syncing) "Syncing..." else "Sync from Cloud")
                }

                if (showUrlEdit) {
                    OutlinedTextField(
                        value = state.syncUrl,
                        onValueChange = { vm.setSyncUrl(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Apps Script URL") },
                        label = { Text("Sync URL") }
                    )
                }

                TextButton(
                    onClick = { showUrlEdit = !showUrlEdit },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showUrlEdit) "Hide link settings" else "Edit sync link")
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                Text(
                    "Manual Import/Export",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "One pair per line, separated by a comma, slash or pipe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 240.dp),
                    placeholder = { Text("Pizza, Pasta\nCoffee, Tea") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { text = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear text") }
                    TextButton(
                        onClick = { vm.clearCustomPairs(); text = ""; status = "All custom pairs wiped." },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Wipe all pairs") }
                }
                status?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (it.contains("successful", true)) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            it, 
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.contains("successful", true))
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                status = vm.importPairs(text).message()
            }) { Text("Import manual text") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
