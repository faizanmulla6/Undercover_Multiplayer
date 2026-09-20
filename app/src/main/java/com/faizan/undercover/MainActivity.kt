package com.faizan.undercover

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faizan.undercover.model.Screen
import com.faizan.undercover.net.NetPhase
import com.faizan.undercover.ui.components.HazardStripe
import com.faizan.undercover.ui.components.RulesSheet
import com.faizan.undercover.ui.components.ScoreboardSheet
import com.faizan.undercover.ui.screens.DiscussionScreen
import com.faizan.undercover.ui.screens.RevealScreen
import com.faizan.undercover.ui.screens.SetupScreen
import com.faizan.undercover.ui.screens.multi.ModeScreen
import com.faizan.undercover.ui.screens.multi.MultiplayerEntryScreen
import com.faizan.undercover.ui.screens.multi.NetGameScreen
import com.faizan.undercover.ui.screens.multi.NetLobbyScreen
import com.faizan.undercover.ui.theme.UndercoverTheme

/** The two ways to play, picked when the app opens. */
enum class AppMode { CHOOSE, SINGLE, MULTI }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A party game gets passed around slowly — don't let the screen sleep mid-round.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val vm: GameViewModel = viewModel()
            val darkMode = vm.darkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
            UndercoverTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UndercoverApp(vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UndercoverApp(
    vm: GameViewModel,
    netVm: MultiplayerViewModel = viewModel()
) {
    val state = vm.state
    var mode by remember { mutableStateOf(AppMode.CHOOSE) }
    var showRules by remember { mutableStateOf(false) }
    var showScores by remember { mutableStateOf(false) }

    BackHandler(enabled = mode != AppMode.CHOOSE) {
        when (mode) {
            AppMode.SINGLE -> when (state.screen) {
                Screen.REVEAL -> if (state.revealIndex > 0) vm.previousReveal() else vm.backToSetup()
                Screen.DISCUSSION -> vm.backToSetup()
                Screen.SETUP -> mode = AppMode.CHOOSE
            }

            AppMode.MULTI -> when (netVm.role) {
                NetRole.HOST -> netVm.stopHosting()
                NetRole.CLIENT -> netVm.leaveGame()
                NetRole.NONE -> mode = AppMode.CHOOSE
            }

            AppMode.CHOOSE -> Unit
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text("UNDERCOVER", style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        IconButton(onClick = { showRules = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "How to play")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.toggleDarkMode() }) {
                            val icon = when (vm.darkMode) {
                                null -> Icons.Default.BrightnessAuto
                                true -> Icons.Default.DarkMode
                                false -> Icons.Default.LightMode
                            }
                            Icon(icon, contentDescription = "Switch theme")
                        }
                        IconButton(onClick = { showScores = true }) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Scoreboard")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HazardStripe(Modifier.padding(horizontal = 16.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 12.dp)
        ) {
            when (mode) {
                AppMode.CHOOSE -> ModeScreen(
                    onSinglePhone = { mode = AppMode.SINGLE },
                    onMultiPhone = { mode = AppMode.MULTI }
                )

                AppMode.SINGLE -> when (state.screen) {
                    Screen.SETUP -> SetupScreen(vm)
                    Screen.REVEAL -> RevealScreen(vm)
                    Screen.DISCUSSION -> DiscussionScreen(vm)
                }

                AppMode.MULTI -> when {
                    netVm.role == NetRole.NONE -> MultiplayerEntryScreen(
                        vm = netVm,
                        onBack = { mode = AppMode.CHOOSE }
                    )

                    netVm.netState.phase == NetPhase.LOBBY -> NetLobbyScreen(netVm)
                    else -> NetGameScreen(netVm)
                }
            }
        }
    }

    if (showRules) RulesSheet(onDismiss = { showRules = false })
    if (showScores) ScoreboardSheet(vm = vm, onDismiss = { showScores = false })
}
