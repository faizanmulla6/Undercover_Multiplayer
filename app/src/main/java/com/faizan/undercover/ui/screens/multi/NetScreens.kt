package com.faizan.undercover.ui.screens.multi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.faizan.undercover.MultiplayerViewModel
import com.faizan.undercover.NetRole
import com.faizan.undercover.net.NetPhase
import com.faizan.undercover.net.NetPlayerView
import com.faizan.undercover.ui.components.SectionCard
import com.faizan.undercover.ui.components.StatPill
import com.faizan.undercover.ui.theme.StencilLabel
import kotlinx.coroutines.delay

/**
 * Lobby. The host sees the controls; everyone else sees who's in and waits.
 */
@Composable
fun NetLobbyScreen(
    vm: MultiplayerViewModel,
    modifier: Modifier = Modifier
) {
    val state = vm.netState
    val isHost = vm.role == NetRole.HOST

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isHost) {
            SectionCard(title = "Invite the others") {
                Text(
                    "On their phones: Play on multiple phones → Find a game. " +
                        "If nothing shows up, they can type this in instead:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    vm.hostAddress?.let { "$it:${vm.hostPort}" } ?: "Checking Wi-Fi…",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            SectionCard(title = "Connected") {
                Text(
                    "You're in ${state.hostName.ifBlank { "the" }}'s game as ${state.self?.name ?: vm.myName}. " +
                        "The host sets everything up — sit tight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard(title = "Players (${state.players.size})") {
            if (state.players.isEmpty()) {
                Text(
                    "Nobody yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.players.forEach { player ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(player.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            when {
                                player.isHost -> "Host"
                                !player.connected -> "Disconnected"
                                else -> "Ready to play"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (player.points > 0) {
                        AssistChip(onClick = {}, label = { Text("${player.points} pts") })
                    }
                    if (isHost && !player.isHost) {
                        IconButton(onClick = { vm.removePlayer(player.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove ${player.name}")
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        if (isHost) {
            SectionCard(title = "Round settings") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Undercover agents", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Auto-balanced as people join — override any time",
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
                        ) { Text("–") }
                        Text(
                            state.undercoverCount.toString(),
                            style = MaterialTheme.typography.titleLarge
                        )
                        FilledTonalIconButton(
                            onClick = { vm.setUndercoverCount(state.undercoverCount + 1) },
                            enabled = state.undercoverCount < vm.maxUndercover()
                        ) { Text("+") }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Blank agent", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "One player gets no word and must bluff",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.useBlank, onCheckedChange = { vm.setUseBlank(it) })
                }

                HorizontalDivider()

                Text("Discussion timer", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 60, 90, 120).forEach { seconds ->
                        FilterChip(
                            selected = state.timerSeconds == seconds,
                            onClick = { vm.setTimerSeconds(seconds) },
                            label = { Text(if (seconds == 0) "Off" else "${seconds}s") }
                        )
                    }
                }
            }

            val blocker = vm.startBlocker()
            if (blocker != null) {
                Text(
                    blocker,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { vm.startRound() },
                enabled = blocker == null,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Deal the round") }

            OutlinedButton(
                onClick = { vm.stopHosting() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("End the game") }
        } else {
            OutlinedButton(
                onClick = { vm.leaveGame() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Leave the game") }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * The round itself. Every phone renders the same screen from the state the host
 * pushed; only the host gets the control row at the bottom.
 */
@Composable
fun NetGameScreen(
    vm: MultiplayerViewModel,
    modifier: Modifier = Modifier
) {
    val state = vm.netState
    val self = state.self
    val isHost = self?.isHost == true
    var guess by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("Round", state.round.toString(), Modifier.weight(1f))
            StatPill("Still in", state.alivePlayers.size.toString(), Modifier.weight(1f))
            StatPill("Out", (state.players.size - state.alivePlayers.size).toString(), Modifier.weight(1f))
        }

        if (state.message.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    state.message,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        when (state.phase) {
            NetPhase.REVEAL -> RevealSection(vm, state.readyCount, state.players.size)
            NetPhase.DISCUSSION -> DiscussionSection(state.order, state.timerSeconds, isHost)
            NetPhase.VOTING -> VotingSection(vm)
            NetPhase.RESULT -> ResultSection(state.outcomeTitle, state.outcomeDetail, state.civilianWord, state.undercoverWord, state.players)
            NetPhase.LOBBY -> Unit
        }

        if (state.phase != NetPhase.VOTING && state.phase != NetPhase.RESULT) {
            SectionCard(title = "At the table") {
                state.players.forEach { player -> PlayerRow(player) }
            }
        }

        if (isHost) {
            HostControls(vm)
        } else {
            OutlinedButton(
                onClick = { vm.leaveGame() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Leave the game") }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (self?.needsGuess == true) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("You were voted out") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("You're the blank agent. Name the civilians' word and you steal the round.")
                    OutlinedTextField(
                        value = guess,
                        onValueChange = { guess = it },
                        singleLine = true,
                        label = { Text("Your guess") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.submitGuess(guess); guess = "" },
                    enabled = guess.isNotBlank()
                ) { Text("Lock it in") }
            },
            dismissButton = {
                TextButton(onClick = { vm.submitGuess(""); guess = "" }) { Text("No guess") }
            }
        )
    }
}

@Composable
private fun RevealSection(vm: MultiplayerViewModel, readyCount: Int, total: Int) {
    val self = vm.netState.self ?: return
    val haptics = LocalHapticFeedback.current
    var held by remember(self.role, self.word, vm.netState.round) { mutableStateOf(false) }
    var seen by remember(self.role, self.word, vm.netState.round) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (held) 180f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "netCardFlip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .pointerInput(self.id, vm.netState.round) {
                detectTapGestures(onPress = {
                    held = true
                    seen = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    tryAwaitRelease()
                    held = false
                })
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation > 90f) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer { if (rotation > 90f) rotationY = 180f },
            contentAlignment = Alignment.Center
        ) {
            if (rotation > 90f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (self.role == "Blank") {
                        Text(
                            "NO WORD",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Bluff from what the others say. If you're voted out you get " +
                                "one guess at the civilians' word.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            self.word,
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Describe it in one short phrase — never say the word itself.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.height(44.dp).width(44.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        if (seen) "ORDERS SEALED AGAIN" else "HOLD TO VIEW YOUR ORDERS",
                        style = StencilLabel,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    Button(
        onClick = { vm.markReady() },
        enabled = seen && !self.ready,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(if (self.ready) "Waiting for the others…" else "I've got it")
    }

    Text(
        "$readyCount of $total ready",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DiscussionSection(order: List<String>, timerSeconds: Int, isHost: Boolean) {
    SectionCard(title = "Speaking order") {
        order.forEachIndexed { index, name ->
            Text(
                "${index + 1}.  $name",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            "One short clue each, in this order. No repeats, never the word itself.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (isHost && timerSeconds > 0) {
        HostTimer(timerSeconds)
    }
}

@Composable
private fun HostTimer(totalSeconds: Int) {
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
private fun VotingSection(vm: MultiplayerViewModel) {
    val state = vm.netState
    val self = state.self
    val canVote = self != null && self.alive

    SectionCard(title = "Vote someone out") {
        Text(
            when {
                self == null -> ""
                !self.alive -> "You're out of this round — sit back and watch."
                self.votedFor != null -> "You voted for ${state.players.find { it.id == self.votedFor }?.name}. Tap again to change or undo."
                else -> "Tap the agent you suspect. One vote each, host will resolve."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        state.players.chunked(2).forEach { rowPlayers ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowPlayers.forEach { player ->
                    val isSelected = self?.votedFor == player.id
                    VoteTile(
                        player = player,
                        selected = isSelected,
                        enabled = canVote && player.alive && player.id != self?.id,
                        onClick = { 
                            if (isSelected) vm.castVote(null) // Undo
                            else vm.castVote(player.id) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPlayers.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        HorizontalDivider()
        Text(
            "${state.votesCast} of ${state.votesNeeded} votes in",
            style = StencilLabel,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun VoteTile(
    player: NetPlayerView,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(82.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                !player.alive -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Box(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (player.alive) null else TextDecoration.LineThrough
                )
                Text(
                    player.revealedRole ?: if (player.hasVoted) "Voted" else "Thinking…",
                    style = StencilLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (player.votesAgainst > 0 && player.alive) {
                Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                    Text(player.votesAgainst.toString())
                }
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Your vote",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ResultSection(
    title: String?,
    detail: String?,
    civilianWord: String?,
    undercoverWord: String?,
    players: List<NetPlayerView>
) {
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
                (title ?: "Round over").uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (civilianWord != null && undercoverWord != null) {
                HorizontalDivider()
                Text(
                    "Civilian word: $civilianWord   ·   Decoy word: $undercoverWord",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    SectionCard(title = "Who was who") {
        players.forEach { player ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(player.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    player.revealedRole ?: "—",
                    style = StencilLabel,
                    color = if (player.revealedRole == "Civilian") {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.height(14.dp).width(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        " ${player.points}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PlayerRow(player: NetPlayerView) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                player.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (player.alive) null else TextDecoration.LineThrough
            )
            Text(
                when {
                    !player.connected -> "Disconnected"
                    player.revealedRole != null -> player.revealedRole
                    player.ready -> "Ready"
                    else -> "In play"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (player.ready) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun HostControls(vm: MultiplayerViewModel) {
    val state = vm.netState
    SectionCard(title = "Host controls") {
        when (state.phase) {
            NetPhase.REVEAL -> {
                val allReady = state.readyCount == state.players.size
                Button(
                    onClick = { vm.beginDiscussion() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (allReady) "Start the discussion" else "Start anyway (${state.readyCount}/${state.players.size} ready)")
                }
            }

            NetPhase.DISCUSSION -> {
                Button(
                    onClick = { vm.openVoting() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Open voting") }
                OutlinedButton(
                    onClick = { vm.revealAll() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reveal all roles") }
            }

            NetPhase.VOTING -> {
                Button(
                    onClick = { vm.processVotes() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Eliminate target") }
                OutlinedButton(
                    onClick = { vm.backToDiscussion() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reopen discussion") }
                OutlinedButton(
                    onClick = { vm.revealAll() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reveal all roles") }
            }

            NetPhase.RESULT -> {
                Button(
                    onClick = { vm.newRound() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Deal another round") }
                OutlinedButton(
                    onClick = { vm.returnToLobby() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Back to lobby") }
            }

            NetPhase.LOBBY -> Unit
        }
        OutlinedButton(
            onClick = { vm.stopHosting() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("End the game for everyone") }
    }
}
