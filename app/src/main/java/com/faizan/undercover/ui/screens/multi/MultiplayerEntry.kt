package com.faizan.undercover.ui.screens.multi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.faizan.undercover.MultiplayerViewModel
import com.faizan.undercover.net.Proto
import com.faizan.undercover.ui.components.SectionCard
import com.faizan.undercover.ui.theme.StencilLabel

/** First thing the app shows: one phone for everyone, or a phone each. */
@Composable
fun ModeScreen(
    onSinglePhone: () -> Unit,
    onMultiPhone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "How are you playing tonight?",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Both modes play the same game — the difference is whether the group " +
                "shares one screen or everyone holds their own.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ModeCard(
            title = "Play on one phone",
            body = "Pass the device around the table. Each player holds the card to " +
                "read their word, then hands it on.",
            hint = "No network needed",
            onClick = onSinglePhone
        )

        ModeCard(
            title = "Play on multiple phones",
            body = "One person hosts, everyone else joins over the same Wi-Fi. Your " +
                "word stays on your own screen and you vote from your own phone.",
            hint = "Everyone on one Wi-Fi network",
            onClick = onMultiPhone
        )
    }
}

@Composable
private fun ModeCard(title: String, body: String, hint: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (title.contains("one phone")) Icons.Default.PhoneAndroid else Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                hint.uppercase(),
                style = StencilLabel,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Host or join, with the name everyone else will see. */
@Composable
fun MultiplayerEntryScreen(
    vm: MultiplayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(vm.myName) }
    var joining by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(title = "Your name") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(18) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Shown to the other players") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }

        vm.status?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { vm.clearStatus() }) { Text("OK") }
                }
            }
        }

        if (!joining) {
            SectionCard(title = "Start a game") {
                Text(
                    "You set the number of undercover agents, deal the round and " +
                        "control when voting opens. Everyone else just sees their card.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { vm.startHosting(name) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Host a game") }
            }

            SectionCard(title = "Join a game") {
                Text(
                    "Make sure you're on the same Wi-Fi as the host.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        joining = true
                        vm.startDiscovery()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Find a game") }
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        } else {
            JoinPanel(
                vm = vm,
                name = name,
                onCancel = {
                    joining = false
                    vm.stopDiscovery()
                }
            )
        }
    }
}

@Composable
private fun JoinPanel(
    vm: MultiplayerViewModel,
    name: String,
    onCancel: () -> Unit
) {
    var manualIp by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { vm.stopDiscovery() }
    }

    SectionCard(title = "Games on this network") {
        if (vm.discoveredHosts.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp).width(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Searching…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            vm.discoveredHosts.forEach { host ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.joinGame(name, host.address, host.port) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(host.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${host.address}:${host.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("Join", style = StencilLabel, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { vm.startDiscovery() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, Modifier.height(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Search again")
        }
    }

    SectionCard(title = "Or type the host's address") {
        Text(
            "The host screen shows this number. Use it if the search comes up empty — " +
                "some routers block automatic discovery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = manualIp,
            onValueChange = { manualIp = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("e.g. 192.168.1.5") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                vm.joinGame(name, manualIp.trim().substringBefore(':'), portFrom(manualIp))
            })
        )
        Button(
            onClick = {
                vm.joinGame(name, manualIp.trim().substringBefore(':'), portFrom(manualIp))
            },
            enabled = manualIp.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Connect") }
    }

    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text("Back")
    }

    Text(
        "Everyone must be on the same Wi-Fi. Mobile data or guest networks that " +
            "isolate devices won't work.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun portFrom(input: String): Int {
    val part = input.substringAfter(':', "").trim()
    return part.toIntOrNull() ?: Proto.PORT
}
