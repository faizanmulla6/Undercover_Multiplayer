package com.faizan.undercover

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faizan.undercover.data.GameStore
import com.faizan.undercover.data.WordBank
import com.faizan.undercover.model.GameState
import com.faizan.undercover.model.HistoryEntry
import com.faizan.undercover.model.ImportResult
import com.faizan.undercover.model.Role
import com.faizan.undercover.model.RoundOutcome
import com.faizan.undercover.model.RoundPlayer
import com.faizan.undercover.model.Screen
import com.faizan.undercover.model.VoteStyle
import com.faizan.undercover.model.WordMode
import com.faizan.undercover.model.WordPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val store = GameStore(app)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    var state by mutableStateOf(GameState())
        private set

    var darkMode by mutableStateOf<Boolean?>(null)
        private set

    init {
        state = state.copy(
            players = store.loadPlayers().take(MAX_PLAYERS),
            scores = store.loadScores(),
            customPairs = store.loadCustomPairs(),
            wordMode = store.loadWordMode(),
            disabledCategories = store.loadDisabledCategories(),
            undercoverCount = store.loadUndercoverCount().coerceAtLeast(1),
            useBlank = store.loadUseBlank(),
            timerSeconds = store.loadTimerSeconds(),
            voteStyle = store.loadVoteStyle(),
            syncUrl = store.loadSyncUrl().ifEmpty { state.syncUrl }
        )
        darkMode = store.loadDarkMode()
        clampUndercover()
    }

    fun toggleDarkMode() {
        darkMode = when (darkMode) {
            null -> true
            true -> false
            false -> null
        }
        store.saveDarkMode(darkMode)
    }


    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    /** Returns an error message, or null when the player was added. */
    fun addPlayer(rawName: String): String? {
        val name = rawName.trim().take(NAME_LIMIT)
        if (name.isEmpty()) return null
        if (state.players.any { it.equals(name, ignoreCase = true) }) {
            return "$name is already on the list."
        }
        if (state.players.size >= MAX_PLAYERS) return "Maximum $MAX_PLAYERS agents."
        state = state.copy(players = state.players + name)
        store.savePlayers(state.players)
        autoBalance()
        clampUndercover()
        return null
    }

    fun removePlayer(index: Int) {
        if (index !in state.players.indices) return
        state = state.copy(players = state.players.toMutableList().apply { removeAt(index) })
        store.savePlayers(state.players)
        autoBalance()
        clampUndercover()
    }

    fun autoBalance() {
        val n = state.players.size
        val count = when {
            n >= 13 -> 4
            n >= 9 -> 3
            n >= 6 -> 2
            else -> 1
        }
        state = state.copy(undercoverCount = count)
        store.saveUndercoverCount(count)
    }

    fun clearPlayers() {
        state = state.copy(players = emptyList())
        store.savePlayers(state.players)
        clampUndercover()
    }

    fun setUndercoverCount(value: Int) {
        val clamped = value.coerceIn(1, state.maxUndercover)
        state = state.copy(undercoverCount = clamped)
        store.saveUndercoverCount(clamped)
    }

    fun setUseBlank(value: Boolean) {
        state = state.copy(useBlank = value)
        store.saveUseBlank(value)
        clampUndercover()
    }

    fun setWordMode(mode: WordMode) {
        state = state.copy(wordMode = mode)
        store.saveWordMode(mode)
    }

    fun toggleCategory(category: String) {
        val disabled = state.disabledCategories.toMutableSet()
        if (!disabled.add(category)) disabled.remove(category)
        state = state.copy(disabledCategories = disabled)
        store.saveDisabledCategories(disabled)
    }

    fun setTimerSeconds(seconds: Int) {
        state = state.copy(timerSeconds = seconds)
        store.saveTimerSeconds(seconds)
    }

    fun setVoteStyle(style: VoteStyle) {
        state = state.copy(voteStyle = style)
        store.saveVoteStyle(style)
    }

    fun setSyncUrl(url: String) {
        state = state.copy(syncUrl = url)
        store.saveSyncUrl(url)
    }

    fun syncFromUrl(onResult: (String) -> Unit = {}) {
        val url = state.syncUrl.trim()
        if (url.isEmpty() || state.syncing) return

        state = state.copy(syncing = true)
        viewModelScope.launch {
            var errorMessage: String? = null
            var responseData: String? = null
            
            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                        .header("Accept", "text/plain, */*")
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            responseData = response.body?.string()
                            if (responseData.isNullOrBlank()) {
                                errorMessage = "The sheet appears to be empty."
                            }
                        } else {
                            errorMessage = "Server returned error ${response.code}${if (response.message.isNotBlank()) ": ${response.message}" else ""}"
                        }
                    }
                } catch (e: java.net.UnknownHostException) {
                    errorMessage = "No internet connection or invalid URL domain."
                } catch (e: java.net.SocketTimeoutException) {
                    errorMessage = "The connection timed out. Try again."
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "An unexpected error occurred."
                }
            }

            state = state.copy(syncing = false)
            val finalData = responseData
            if (finalData != null) {
                // Check if the data looks like word pairs (contains at least one separator)
                if (finalData.contains(",") || finalData.contains("|") || finalData.contains("/")) {
                    val report = importPairs(finalData)
                    onResult("Sync successful! ${report.message()}")
                } else {
                    onResult("Sync failed: The sheet didn't contain any word pairs in the correct format.")
                }
            } else {
                onResult("Sync failed: $errorMessage")
            }
        }
    }

    private fun clampUndercover() {
        val clamped = state.undercoverCount.coerceIn(1, state.maxUndercover)
        if (clamped != state.undercoverCount) {
            state = state.copy(undercoverCount = clamped)
            store.saveUndercoverCount(clamped)
        }
    }

    // ------------------------------------------------------------------
    // Word bank
    // ------------------------------------------------------------------

    fun wordPool(): List<WordPair> = when (state.wordMode) {
        WordMode.BUILTIN -> WordBank.byCategory(state.disabledCategories)
        WordMode.MIX -> WordBank.byCategory(state.disabledCategories) + state.customPairs
        WordMode.CUSTOM -> state.customPairs
    }

    /** Returns an error message, or null when the pair was added. */
    fun addCustomPair(a: String, b: String): String? {
        val first = a.trim().take(WORD_LIMIT)
        val second = b.trim().take(WORD_LIMIT)
        if (first.isEmpty() || second.isEmpty()) return "Enter both words."
        if (first.equals(second, ignoreCase = true)) return "The two words need to be different."
        val exists = state.customPairs.any {
            it.civilian.equals(first, true) && it.undercover.equals(second, true)
        }
        if (exists) return "That pair is already in your list."
        state = state.copy(customPairs = state.customPairs + WordPair(first, second))
        store.saveCustomPairs(state.customPairs)
        return null
    }

    fun removeCustomPair(index: Int) {
        if (index !in state.customPairs.indices) return
        state = state.copy(
            customPairs = state.customPairs.toMutableList().apply { removeAt(index) }
        )
        store.saveCustomPairs(state.customPairs)
    }

    fun clearCustomPairs() {
        state = state.copy(customPairs = emptyList())
        store.saveCustomPairs(emptyList())
    }

    fun exportPairs(): String =
        state.customPairs.joinToString("\n") { "${it.civilian}, ${it.undercover}" }

    fun importPairs(text: String): ImportResult {
        var added = 0
        var duplicates = 0
        var invalid = 0

        val working = state.customPairs.toMutableList()
        val seen = working
            .map { it.civilian.lowercase() + "|" + it.undercover.lowercase() }
            .toMutableSet()

        text.split("\n").forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            val split = splitPairLine(trimmed)
            if (split == null) {
                invalid++
                return@forEach
            }
            val (a, b) = split
            if (a.isEmpty() || b.isEmpty() || a.equals(b, ignoreCase = true)) {
                invalid++
                return@forEach
            }
            val key = a.lowercase() + "|" + b.lowercase()
            if (!seen.add(key)) {
                duplicates++
                return@forEach
            }
            working += WordPair(a.take(WORD_LIMIT), b.take(WORD_LIMIT))
            added++
        }

        if (added > 0) {
            state = state.copy(customPairs = working)
            store.saveCustomPairs(working)
        }
        return ImportResult(added, duplicates, invalid)
    }

    private fun splitPairLine(line: String): Pair<String, String>? {
        listOf(",", "|", "/", "\t", " - ").forEach { separator ->
            val index = line.indexOf(separator)
            if (index != -1) {
                return line.substring(0, index).trim() to
                    line.substring(index + separator.length).trim()
            }
        }
        return null
    }

    // ------------------------------------------------------------------
    // Round lifecycle
    // ------------------------------------------------------------------

    fun canStart(): Boolean {
        val n = state.players.size
        val civilians = n - state.undercoverCount - if (state.useBlank) 1 else 0
        return n >= state.minPlayers && civilians >= 2 && wordPool().isNotEmpty()
    }

    fun setupBlocker(): String? {
        val n = state.players.size
        if (n < state.minPlayers) {
            return "Add at least ${state.minPlayers} agents to start."
        }
        val civilians = n - state.undercoverCount - if (state.useBlank) 1 else 0
        if (civilians < 2) return "Keep at least two civilians in the game."
        if (wordPool().isEmpty()) {
            return if (state.wordMode == WordMode.CUSTOM) {
                "Add a custom pair, or switch the word bank back to built-in."
            } else {
                "Every category is switched off — turn one back on."
            }
        }
        return null
    }

    fun startRound() {
        if (!canStart()) return
        val pool = wordPool()
        val pair = pool.random()
        val flip = (0..1).random() == 0
        val civilianWord = if (flip) pair.civilian else pair.undercover
        val undercoverWord = if (flip) pair.undercover else pair.civilian

        val roles = MutableList(state.players.size) { Role.CIVILIAN }
        val shuffled = state.players.indices.shuffled()
        var cursor = 0
        repeat(state.undercoverCount) {
            roles[shuffled[cursor]] = Role.UNDERCOVER
            cursor++
        }
        if (state.useBlank) {
            roles[shuffled[cursor]] = Role.BLANK
            cursor++
        }

        val round = state.players.mapIndexed { index, name ->
            RoundPlayer(
                name = name,
                role = roles[index],
                word = when (roles[index]) {
                    Role.CIVILIAN -> civilianWord
                    Role.UNDERCOVER -> undercoverWord
                    Role.BLANK -> ""
                }
            )
        }

        // A civilian always opens the round, so the blank agent never has to
        // speak first with nothing to go on.
        val civilianIndices = state.players.indices.filter { roles[it] == Role.CIVILIAN }.shuffled()
        val opener = civilianIndices.first()
        val others = state.players.indices.filter { it != opener }.shuffled()
        val speakingOrder = listOf(opener) + others

        state = state.copy(
            screen = Screen.REVEAL,
            roundNumber = state.roundNumber + 1,
            round = round,
            civilianWord = civilianWord,
            undercoverWord = undercoverWord,
            revealIndex = 0,
            speakingOrder = speakingOrder,
            votes = emptyMap(),
            pendingBlankGuess = null,
            undoSnapshot = null,
            outcome = null
        )
    }

    fun nextReveal() {
        if (state.revealIndex < state.round.lastIndex) {
            state = state.copy(revealIndex = state.revealIndex + 1)
        } else {
            state = state.copy(screen = Screen.DISCUSSION)
        }
    }

    fun previousReveal() {
        if (state.revealIndex > 0) {
            state = state.copy(revealIndex = state.revealIndex - 1)
        }
    }

    fun backToSetup() {
        state = state.copy(screen = Screen.SETUP, votes = emptyMap())
    }

    fun newRound() = startRound()

    // ------------------------------------------------------------------
    // Voting
    // ------------------------------------------------------------------

    fun addVote(index: Int) {
        if (state.roundOver) return
        val player = state.round.getOrNull(index) ?: return
        if (player.eliminated) return
        val votes = state.votes.toMutableMap()
        votes[index] = (votes[index] ?: 0) + 1
        state = state.copy(votes = votes)
    }

    fun removeVote(index: Int) {
        val current = state.votes[index] ?: return
        val votes = state.votes.toMutableMap()
        if (current <= 1) votes.remove(index) else votes[index] = current - 1
        state = state.copy(votes = votes)
    }

    fun clearVotes() {
        state = state.copy(votes = emptyMap())
    }

    /** The single player with the most votes, or null on a tie / no votes. */
    fun voteLeader(): Int? {
        val top = state.votes.maxByOrNull { it.value } ?: return null
        val tied = state.votes.count { it.value == top.value }
        return if (tied == 1) top.key else null
    }

    fun eliminate(index: Int) {
        if (state.roundOver) return
        val player = state.round.getOrNull(index) ?: return
        if (player.eliminated) return

        val snapshot = state.round
        val updated = state.round.toMutableList()
        updated[index] = player.copy(eliminated = true)
        state = state.copy(round = updated, votes = emptyMap(), undoSnapshot = snapshot)

        if (player.role == Role.BLANK) {
            // The blank agent gets one shot at naming the civilian word.
            state = state.copy(pendingBlankGuess = index)
            return
        }
        checkEnd()
    }

    fun undoElimination() {
        val snapshot = state.undoSnapshot ?: return
        if (state.roundOver) return
        state = state.copy(
            round = snapshot,
            undoSnapshot = null,
            pendingBlankGuess = null,
            votes = emptyMap()
        )
    }

    fun submitBlankGuess(guess: String) {
        val index = state.pendingBlankGuess ?: return
        val player = state.round[index]
        val correct = guess.trim().equals(state.civilianWord.trim(), ignoreCase = true)
        state = state.copy(pendingBlankGuess = null)
        if (correct) {
            finish(
                RoundOutcome(
                    title = "Blank agent wins",
                    detail = "${player.name} was voted out, guessed \"${state.civilianWord}\" and took the round.",
                    points = mapOf(player.name to BLANK_GUESS_POINTS)
                )
            )
        } else {
            checkEnd()
        }
    }

    fun finish(outcome: RoundOutcome) {
        val scores = state.scores.toMutableMap()
        outcome.points.forEach { (name, points) ->
            scores[name] = (scores[name] ?: 0) + points
        }
        if (outcome.points.isNotEmpty()) store.saveScores(scores)

        val entry = HistoryEntry(
            roundNumber = state.roundNumber,
            title = outcome.title,
            civilianWord = state.civilianWord,
            undercoverWord = state.undercoverWord,
            players = state.round
        )
        state = state.copy(
            outcome = outcome,
            scores = scores,
            votes = emptyMap(),
            undoSnapshot = null,
            history = state.history + entry
        )
    }

    fun skipBlankGuess() {
        state = state.copy(pendingBlankGuess = null)
        checkEnd()
    }

    private fun checkEnd() {
        val civilians = state.aliveCivilians
        val agents = state.aliveAgents
        when {
            agents == 0 -> finish(
                RoundOutcome(
                    title = "Civilians win",
                    detail = "Every undercover agent has been caught.",
                    points = state.round
                        .filter { !it.eliminated && it.isCivilian }
                        .associate { it.name to CIVILIAN_POINTS }
                )
            )

            agents >= civilians -> finish(
                RoundOutcome(
                    title = "Undercover wins",
                    detail = "The undercover side now equals or outnumbers the civilians.",
                    points = state.round
                        .filter { !it.eliminated && !it.isCivilian }
                        .associate { it.name to UNDERCOVER_POINTS }
                )
            )
        }
    }

    fun revealAll() {
        if (state.roundOver) return
        finish(
            RoundOutcome(
                title = "Case file closed",
                detail = "Roles revealed on request — no points awarded.",
                points = emptyMap(),
                scored = false
            )
        )
    }

    // ------------------------------------------------------------------
    // Scores
    // ------------------------------------------------------------------

    fun leaderboard(): List<Pair<String, Int>> {
        val names = LinkedHashSet<String>()
        names += state.players
        names += state.scores.keys
        return names
            .map { it to (state.scores[it] ?: 0) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }

    fun resetScores() {
        state = state.copy(scores = emptyMap(), history = emptyList())
        store.saveScores(emptyMap())
    }

    companion object {
        const val MAX_PLAYERS = 16
        const val NAME_LIMIT = 18
        const val WORD_LIMIT = 24
        const val CIVILIAN_POINTS = 2
        const val UNDERCOVER_POINTS = 3
        const val BLANK_GUESS_POINTS = 4
    }
}
