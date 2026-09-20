package com.faizan.undercover.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.faizan.undercover.GameViewModel
import com.faizan.undercover.model.Role
import com.faizan.undercover.ui.theme.StencilLabel

@Composable
fun RevealScreen(
    vm: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state = vm.state
    val index = state.revealIndex
    val player = state.round.getOrNull(index) ?: return
    val haptics = LocalHapticFeedback.current

    // Reset per player: the card can only be opened while a finger is held down,
    // so a phone put on the table never leaves a word on screen.
    var held by remember(index) { mutableStateOf(false) }
    var seen by remember(index) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (held) 180f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "cardFlip"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "AGENT ${index + 1} OF ${state.round.size}",
                style = StencilLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                player.name,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            LinearProgressIndicator(
                progress = { (index + 1f) / state.round.size },
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            Text(
                "Make sure nobody else can see the screen, then press and hold the card.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .pointerInput(index) {
                    detectTapGestures(
                        onPress = {
                            held = true
                            seen = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            held = false
                        }
                    )
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
                    RevealedFace(player.role, player.word)
                } else {
                    HiddenFace(seen)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { vm.previousReveal() },
                enabled = index > 0,
                modifier = Modifier.weight(1f).height(54.dp)
            ) { Text("Back") }

            Button(
                onClick = { vm.nextReveal() },
                enabled = seen,
                modifier = Modifier.weight(2f).height(54.dp)
            ) {
                Text(
                    if (index == state.round.lastIndex) "Start discussion" else "Pass to next agent"
                )
            }
        }

        Text(
            if (seen) "Hand the phone on before letting go of a secret."
            else "Hold the card to read your orders.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HiddenFace(seen: Boolean) {
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
            if (seen) "ORDERS SEALED AGAIN" else "HOLD TO VIEW ORDERS",
            style = StencilLabel,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun RevealedFace(role: Role, word: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            Icons.Default.Visibility,
            contentDescription = null,
            modifier = Modifier.height(28.dp).width(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        if (role == Role.BLANK) {
            Text(
                "NO WORD",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                "You are the blank agent. Listen closely, bluff a clue, and blend in. " +
                    "If you are voted out you get one guess at the civilian word.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                word,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Text(
                "Remember it. Describe it in one short phrase without ever saying the word.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
