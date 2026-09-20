package com.faizan.undercover.model

/** The three role cards that can be dealt in a round. */
enum class Role { CIVILIAN, UNDERCOVER, BLANK }

/** Where secret words are drawn from. */
enum class WordMode { BUILTIN, MIX, CUSTOM }

/** Top level destinations. The game is linear, so a simple enum beats a nav graph here. */
enum class Screen { SETUP, REVEAL, DISCUSSION }

/** How the group decides who leaves: a straight tap, or a counted vote. */
enum class VoteStyle { QUICK, TALLY }

data class WordPair(
    val civilian: String,
    val undercover: String,
    val category: String = CUSTOM_CATEGORY
) {
    companion object {
        const val CUSTOM_CATEGORY = "Custom"
    }
}

/** One player as dealt into the current round. */
data class RoundPlayer(
    val name: String,
    val role: Role,
    val word: String,
    val eliminated: Boolean = false
) {
    val isCivilian: Boolean get() = role == Role.CIVILIAN
}

/** The result of a finished round, including what each player scored. */
data class RoundOutcome(
    val title: String,
    val detail: String,
    val points: Map<String, Int> = emptyMap(),
    val scored: Boolean = true
)

data class HistoryEntry(
    val roundNumber: Int,
    val title: String,
    val civilianWord: String,
    val undercoverWord: String,
    val players: List<RoundPlayer>
)

data class GameState(
    // Setup
    val screen: Screen = Screen.SETUP,
    val players: List<String> = emptyList(),
    val undercoverCount: Int = 1,
    val useBlank: Boolean = false,
    val wordMode: WordMode = WordMode.BUILTIN,
    val customPairs: List<WordPair> = emptyList(),
    val disabledCategories: Set<String> = emptySet(),
    val timerSeconds: Int = 0,
    val voteStyle: VoteStyle = VoteStyle.QUICK,
    val syncUrl: String = "https://script.google.com/macros/s/AKfycbzUMb4J9XFNg0ysNCY_duo2_8kiOrWwzTxug9Lt9HlvmmUT7LmrRc1g6ldw2UN9XxLv7A/exec",
    val syncing: Boolean = false,

    // Round
    val roundNumber: Int = 0,
    val round: List<RoundPlayer> = emptyList(),
    val civilianWord: String = "",
    val undercoverWord: String = "",
    val revealIndex: Int = 0,
    val speakingOrder: List<Int> = emptyList(),
    val votes: Map<Int, Int> = emptyMap(),
    val pendingBlankGuess: Int? = null,
    val undoSnapshot: List<RoundPlayer>? = null,
    val outcome: RoundOutcome? = null,

    // Persisted across rounds
    val scores: Map<String, Int> = emptyMap(),
    val history: List<HistoryEntry> = emptyList()
) {
    val aliveCivilians: Int
        get() = round.count { !it.eliminated && it.isCivilian }

    val aliveAgents: Int
        get() = round.count { !it.eliminated && !it.isCivilian }

    val maxUndercover: Int
        get() {
            val n = players.size
            val blanks = if (useBlank) 1 else 0
            // Civilians must always keep a majority of at least two.
            return maxOf(1, (n - blanks - 2).coerceAtMost((n - 1) / 2))
        }

    val minPlayers: Int get() = if (useBlank) 4 else 3

    val roundOver: Boolean get() = outcome != null
}

data class ImportResult(
    val added: Int,
    val duplicates: Int,
    val invalid: Int
) {
    fun message(): String {
        val parts = mutableListOf<String>()
        if (added > 0) parts += "$added added"
        if (duplicates > 0) parts += "$duplicates duplicate${if (duplicates == 1) "" else "s"} skipped"
        if (invalid > 0) parts += "$invalid line${if (invalid == 1) "" else "s"} unrecognised"
        return if (parts.isEmpty()) "Nothing to import." else parts.joinToString(", ") + "."
    }
}
