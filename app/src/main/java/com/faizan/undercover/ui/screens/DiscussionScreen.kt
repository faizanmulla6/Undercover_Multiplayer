package com.faizan.undercover.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Undo
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.faizan.undercover.GameViewModel
import com.faizan.undercover.model.Role
import com.faizan.undercover.model.RoundPlayer
import com.faizan.undercover.model.VoteStyle
import com.faizan.undercover.ui.components.SectionCard
import com.faizan.undercover.ui.components.StatPill
import com.faizan.undercover.ui.theme.StencilLabel
import kotlinx.coroutines.delay

private val PlayerColors = listOf(
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
    Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
    Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscussionScreen(
    vm: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state = vm.state
    val haptic = LocalHapticFeedback.current
    var confirmTarget by remember { mutableStateOf<Int?>(null) }
    var blankGuess by remember { mutableStateOf("") }
    var showSecretVoting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("Civilians left", state.aliveCivilians.toString(), Modifier.weight(1f))
            StatPill("Agents left", state.aliveAgents.toString(), Modifier.weight(1f))
            StatPill("Round", state.roundNumber.toString(), Modifier.weight(1f))
        }

        if (!state.roundOver) {
            SectionCard(title = "Speaking order") {
                Text(
                    "Each agent gives one short clue about their word — no repeats, " +
                        "never the word itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.speakingOrder.forEachIndexed { position, playerIndex ->
                        val p = state.round.getOrNull(playerIndex) ?: return@forEachIndexed
                        AssistChip(
                            onClick = {},
                            enabled = !p.eliminated,
                            label = { Text("${position + 1}. ${p.name}") }
                        )
                    }
                }
            }

            if (state.timerSeconds > 0) {
                DiscussionTimer(totalSeconds = state.timerSeconds)
            }
        }

        SectionCard(title = if (state.roundOver) "Case file" else "Vote someone out") {
            if (!state.roundOver) {
                Text(
                    if (state.voteStyle == VoteStyle.QUICK) {
                        "Tap the agent the group suspects, then confirm."
                    } else {
                        "Pass the phone around for secret voting. 1 vote per alive agent."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.speakingOrder.chunked(2).forEach { rowIndices ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowIndices.forEach { index ->
                        val player = state.round[index]
                        val totalCast = vm.totalVotesCast
                        val aliveCount = vm.alivePlayersCount
                        PlayerTile(
                            player = player,
                            index = index,
                            votes = state.votes[index] ?: 0,
                            showRole = player.eliminated || state.roundOver,
                            showVotes = state.voteStyle == VoteStyle.QUICK || totalCast >= aliveCount || state.roundOver,
                            enabled = !player.eliminated && !state.roundOver,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (state.voteStyle == VoteStyle.QUICK) {
                                    if (confirmTarget == index) confirmTarget = null // Undo
                                    else confirmTarget = index
                                } else {
                                    vm.addVote(index)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.removeVote(index)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowIndices.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            if (!state.roundOver && state.voteStyle == VoteStyle.TALLY) {
                val leader = vm.voteLeader()
                val totalCast = vm.totalVotesCast
                val aliveCount = vm.alivePlayersCount

                HorizontalDivider()

                state.tieMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "Votes cast: $totalCast / $aliveCount",
                    style = StencilLabel,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { vm.clearVotes() },
                        enabled = state.votes.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear votes") }

                    Button(
                        onClick = { showSecretVoting = true },
                        enabled = totalCast < aliveCount,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (totalCast == 0) "Secret Vote" else "Continue Vote")
                    }
                }

                if (leader != null) {
                    Button(
                        onClick = { confirmTarget = leader },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminate ${state.round[leader].name} (Most Voted)")
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.roundOver) {
            state.outcome?.let { outcome ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            outcome.title.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            outcome.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        HorizontalDivider()
                        Text(
                            "Civilian word: ${state.civilianWord}   ·   " +
                                "Decoy word: ${state.undercoverWord}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        state.round.forEach { player ->
                            val points = outcome.points[player.name] ?: 0
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    player.name +
                                        if (points > 0) "  +$points" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    roleLabel(player.role),
                                    style = StencilLabel,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { vm.undoElimination() },
                enabled = state.undoSnapshot != null && !state.roundOver,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null, Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Undo")
            }
            OutlinedButton(
                onClick = { vm.revealAll() },
                enabled = !state.roundOver,
                modifier = Modifier.weight(1f)
            ) { Text("Reveal roles") }
        }

        val atLimit = state.targetRounds > 0 && state.roundNumber >= state.targetRounds

        Button(
            onClick = { vm.newRound() },
            enabled = !atLimit || !state.roundOver,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { 
            Text(if (atLimit && state.roundOver) "Session limit reached" else "Deal a new round") 
        }

        if (atLimit && state.roundOver) {
            Text(
                "You have played all ${state.targetRounds} rounds. Check the final scores!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        OutlinedButton(
            onClick = { vm.backToSetup() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back to setup") }

        Spacer(Modifier.height(8.dp))
    }

    // Confirm an elimination before anything is revealed.
    confirmTarget?.let { index ->
        val target = state.round.getOrNull(index)
        if (target == null) {
            confirmTarget = null
        } else {
            AlertDialog(
                onDismissRequest = { confirmTarget = null },
                title = { Text("Vote out ${target.name}?") },
                text = { Text("Their role is revealed to everyone. You can undo one elimination if the group changes its mind.") },
                confirmButton = {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.eliminate(index)
                        confirmTarget = null
                    }) { Text("Eliminate") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmTarget = null }) { Text("Cancel") }
                }
            )
        }
    }

    // The blank agent's last chance.
    state.pendingBlankGuess?.let { index ->
        val target = state.round[index]
        AlertDialog(
            onDismissRequest = { },
            title = { Text("${target.name} was the blank agent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("One guess at the civilian word steals the round outright.")
                    OutlinedTextField(
                        value = blankGuess,
                        onValueChange = { blankGuess = it },
                        singleLine = true,
                        label = { Text("Their guess") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.submitBlankGuess(blankGuess)
                        blankGuess = ""
                    },
                    enabled = blankGuess.isNotBlank()
                ) { Text("Lock it in") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.skipBlankGuess()
                    blankGuess = ""
                }) { Text("No guess") }
            }
        )
    }

    if (showSecretVoting) {
        SecretVotingDialog(
            vm = vm,
            onDismiss = { showSecretVoting = false }
        )
    }
}

@Composable
private fun DiscussionTimer(totalSeconds: Int) {
    var remaining by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }
    var running by remember(totalSeconds) { mutableStateOf(false) }

    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        } else if (remaining == 0) {
            running = false
        }
    }

    SectionCard(title = "Discussion timer") {
        Text(
            "%d:%02d".format(remaining / 60, remaining % 60),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        LinearProgressIndicator(
            progress = { if (totalSeconds == 0) 0f else remaining / totalSeconds.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { running = !running },
                enabled = remaining > 0,
                modifier = Modifier.weight(1f)
            ) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(
                onClick = { running = false; remaining = totalSeconds },
                modifier = Modifier.weight(1f)
            ) { Text("Reset") }
        }
    }
}

@Composable
private fun SecretVotingDialog(
    vm: GameViewModel,
    onDismiss: () -> Unit
) {
    val state = vm.state
    val alivePlayers = remember(state.round) {
        state.round.mapIndexedNotNull { index, player ->
            if (!player.eliminated) index to player else null
        }
    }

    var currentVoterStep by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }

    if (currentVoterStep >= alivePlayers.size || vm.totalVotesCast >= vm.alivePlayersCount) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    val currentVoter = alivePlayers[currentVoterStep]

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                if (!isRevealed) "Pass Phone to Agent ${currentVoterStep + 1}"
                else "${currentVoter.second.name}, cast your vote"
            )
        },
        text = {
            if (!isRevealed) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        currentVoter.second.name,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Hand the phone to ${currentVoter.second.name}. Make sure no one else can see the screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Who do you suspect?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    alivePlayers.forEach { (targetIndex, candidate) ->
                        if (targetIndex != currentVoter.first) {
                            OutlinedButton(
                                onClick = {
                                    vm.addVote(targetIndex)
                                    isRevealed = false
                                    currentVoterStep++
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(candidate.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isRevealed) {
                Button(
                    onClick = { isRevealed = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I am ${currentVoter.second.name}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerTile(
    player: RoundPlayer,
    index: Int,
    votes: Int,
    showRole: Boolean,
    showVotes: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerColor = if (player.eliminated) {
        MaterialTheme.colorScheme.outline
    } else {
        PlayerColors[index % PlayerColors.size]
    }

    Card(
        modifier = modifier
            .height(84.dp)
            .then(
                if (enabled) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (player.eliminated) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Box(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.headlineSmall,
                    textDecoration = if (player.eliminated) TextDecoration.LineThrough else null,
                    color = playerColor
                )
                Text(
                    if (showRole) roleLabel(player.role) else "In play",
                    style = StencilLabel,
                    color = if (showRole && player.role != Role.CIVILIAN && !player.eliminated) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (showVotes && votes > 0 && !player.eliminated) {
                Badge(modifier = Modifier.align(Alignment.TopEnd)) { Text(votes.toString()) }
            }
        }
    }
}

private fun roleLabel(role: Role): String = when (role) {
    Role.CIVILIAN -> "Civilian"
    Role.UNDERCOVER -> "Undercover"
    Role.BLANK -> "Blank"
}
