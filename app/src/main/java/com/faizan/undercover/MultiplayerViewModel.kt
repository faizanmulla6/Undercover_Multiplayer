package com.faizan.undercover

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.faizan.undercover.data.GameStore
import com.faizan.undercover.data.WordBank
import com.faizan.undercover.model.Role
import com.faizan.undercover.model.WordMode
import com.faizan.undercover.model.WordPair
import com.faizan.undercover.net.DiscoveredHost
import com.faizan.undercover.net.Discovery
import com.faizan.undercover.net.GameClient
import com.faizan.undercover.net.GameServer
import com.faizan.undercover.net.NetPhase
import com.faizan.undercover.net.NetPlayerView
import com.faizan.undercover.net.NetSelf
import com.faizan.undercover.net.NetState
import com.faizan.undercover.net.Proto
import org.json.JSONObject

enum class NetRole { NONE, HOST, CLIENT }

/**
 * Everything for "one phone per player" games on a shared Wi-Fi network.
 *
 * The host phone owns the truth: it deals the words, counts the votes and calls
 * the result. Joining phones only ever receive their own card plus the public
 * view of the table, so nobody can peek at someone else's word, and only the
 * host can change the setup.
 */
class MultiplayerViewModel(app: Application) : AndroidViewModel(app) {

    private val store = GameStore(app)
    private val discovery = Discovery(app)

    private var server: GameServer? = null
    private var client: GameClient? = null

    // ---- Observable UI state -------------------------------------------------

    var role by mutableStateOf(NetRole.NONE)
        private set
    var connected by mutableStateOf(false)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var netState by mutableStateOf(NetState())
        private set
    var hostAddress by mutableStateOf<String?>(null)
        private set
    var hostPort by mutableStateOf(Proto.PORT)
        private set
    var discoveredHosts by mutableStateOf<List<DiscoveredHost>>(emptyList())
        private set
    var myName by mutableStateOf("")
        private set

    // ---- Host-side model -----------------------------------------------------

    private class Seat(
        val id: String,
        var name: String,
        var connectionId: String?,
        val isHost: Boolean = false
    ) {
        var alive = true
        var connected = true
        var ready = false
        var role: Role = Role.CIVILIAN
        var word: String = ""
        var votedFor: String? = null
        var points: Int = 0
    }

    private val seats = mutableListOf<Seat>()
    private var seatCounter = 0
    private var phase = NetPhase.LOBBY
    private var roundNumber = 0
    private var civilianWord = ""
    private var undercoverWord = ""
    private var speakingOrder: List<String> = emptyList() // Now stores IDs
    private var headline = ""
    private var outcomeTitle: String? = null
    private var outcomeDetail: String? = null
    private var pendingGuessSeat: String? = null
    private var rolesPublic = false

    private var undercoverCount = 1
    private var useBlank = false
    private var timerSeconds = 0

    // =========================================================================
    // Hosting
    // =========================================================================

    fun startHosting(name: String) {
        val trimmed = name.trim().take(GameViewModel.NAME_LIMIT)
        if (trimmed.isEmpty()) {
            status = "Enter your name first."
            return
        }
        myName = trimmed
        resetHostModel()

        val srv = GameServer(
            onMessage = { connId, msg -> handleClientMessage(connId, msg) },
            onJoined = { /* nothing until they send their name */ },
            onLeft = { connId -> handleClientLeft(connId) },
            onError = { message -> status = message }
        )
        if (!srv.start()) return
        server = srv
        hostPort = srv.port
        hostAddress = GameServer.localIpAddress()

        seats += Seat(id = nextSeatId(), name = trimmed, connectionId = null, isHost = true)
        role = NetRole.HOST
        connected = true
        headline = "Waiting for players to join."
        status = null
        discovery.register("Undercover · $trimmed", srv.port) { message -> status = message }
        pushState()
    }

    fun stopHosting() {
        discovery.shutdown()
        server?.broadcast(Proto.message(Proto.S_KICKED).put("reason", "The host ended the game."))
        server?.stop()
        server = null
        resetHostModel()
        role = NetRole.NONE
        connected = false
        netState = NetState()
        hostAddress = null
        status = null
    }

    private fun resetHostModel() {
        seats.clear()
        seatCounter = 0
        phase = NetPhase.LOBBY
        roundNumber = 0
        civilianWord = ""
        undercoverWord = ""
        speakingOrder = emptyList()
        outcomeTitle = null
        outcomeDetail = null
        pendingGuessSeat = null
        rolesPublic = false
        undercoverCount = 1
        useBlank = false
        timerSeconds = store.loadTimerSeconds()
    }

    private fun nextSeatId(): String {
        seatCounter++
        return "p$seatCounter"
    }

    // ---- Host settings (host-only authority) --------------------------------

    fun setUndercoverCount(value: Int) {
        if (role != NetRole.HOST || phase != NetPhase.LOBBY) return
        undercoverCount = value.coerceIn(1, maxUndercover())
        pushState()
    }

    fun setUseBlank(value: Boolean) {
        if (role != NetRole.HOST || phase != NetPhase.LOBBY) return
        useBlank = value
        undercoverCount = undercoverCount.coerceIn(1, maxUndercover())
        pushState()
    }

    fun setTimerSeconds(value: Int) {
        if (role != NetRole.HOST) return
        timerSeconds = value
        store.saveTimerSeconds(value)
        pushState()
    }

    fun maxUndercover(): Int {
        val n = seats.size
        val blanks = if (useBlank) 1 else 0
        return maxOf(1, (n - blanks - 2).coerceAtMost(maxOf(1, (n - 1) / 2)))
    }

    fun autoBalance() {
        if (role != NetRole.HOST) return
        val n = seats.size
        undercoverCount = when {
            n >= 13 -> 4
            n >= 9 -> 3
            n >= 6 -> 2
            else -> 1
        }.coerceIn(1, maxUndercover())
        pushState()
    }

    fun removePlayer(seatId: String) {
        if (role != NetRole.HOST || phase != NetPhase.LOBBY) return
        val seat = seats.firstOrNull { it.id == seatId } ?: return
        if (seat.isHost) return
        seat.connectionId?.let { connId ->
            server?.send(connId, Proto.message(Proto.S_KICKED).put("reason", "The host removed you."))
            server?.disconnect(connId)
        }
        seats.remove(seat)
        pushState()
    }

    fun startBlocker(): String? {
        val n = seats.size
        val minimum = if (useBlank) 4 else 3
        if (n < minimum) return "Need at least $minimum players — $n connected."
        val civilians = n - undercoverCount - if (useBlank) 1 else 0
        if (civilians < 2) return "Keep at least two civilians in the game."
        if (wordPool().isEmpty()) return "No word pairs available — check the word bank in single-phone setup."
        return null
    }

    private fun wordPool(): List<WordPair> {
        val disabled = store.loadDisabledCategories()
        val custom = store.loadCustomPairs()
        return when (store.loadWordMode()) {
            WordMode.BUILTIN -> WordBank.byCategory(disabled)
            WordMode.MIX -> WordBank.byCategory(disabled) + custom
            WordMode.CUSTOM -> custom.ifEmpty { WordBank.byCategory(disabled) }
        }.ifEmpty { WordBank.pairs }
    }

    // ---- Round flow ----------------------------------------------------------

    fun startRound() {
        if (role != NetRole.HOST) return
        if (startBlocker() != null) return

        val pair = wordPool().random()
        val flip = (0..1).random() == 0
        civilianWord = if (flip) pair.civilian else pair.undercover
        undercoverWord = if (flip) pair.undercover else pair.civilian

        val shuffled = seats.indices.shuffled()
        val assigned = MutableList(seats.size) { Role.CIVILIAN }
        var cursor = 0
        repeat(undercoverCount) {
            assigned[shuffled[cursor]] = Role.UNDERCOVER
            cursor++
        }
        if (useBlank) {
            assigned[shuffled[cursor]] = Role.BLANK
            cursor++
        }

        seats.forEachIndexed { index, seat ->
            seat.role = assigned[index]
            seat.word = when (seat.role) {
                Role.CIVILIAN -> civilianWord
                Role.UNDERCOVER -> undercoverWord
                Role.BLANK -> ""
            }
            seat.alive = true
            seat.ready = false
            seat.votedFor = null
        }

        // A civilian always opens so the blank agent never has to speak blind.
        val civilians = seats.filter { it.role == Role.CIVILIAN }.shuffled()
        val opener = civilians.firstOrNull() ?: seats.random()
        val others = seats.filter { it.id != opener.id }.shuffled()
        speakingOrder = (listOf(opener) + others).map { it.id }

        roundNumber++
        rolesPublic = false
        pendingGuessSeat = null
        outcomeTitle = null
        outcomeDetail = null
        phase = NetPhase.REVEAL
        headline = "Everyone: hold your card to read your orders."
        pushState()
    }

    fun beginDiscussion() {
        if (role != NetRole.HOST || phase != NetPhase.REVEAL) return
        phase = NetPhase.DISCUSSION
        headline = "Give your clue in the order shown."
        pushState()
    }

    fun openVoting() {
        if (role != NetRole.HOST) return
        if (phase != NetPhase.DISCUSSION) return
        seats.forEach { it.votedFor = null }
        pendingGuessSeat = null
        phase = NetPhase.VOTING
        headline = "Vote on your own phone."
        pushState()
    }

    fun backToDiscussion() {
        if (role != NetRole.HOST || phase != NetPhase.VOTING) return
        seats.forEach { it.votedFor = null }
        phase = NetPhase.DISCUSSION
        headline = "Discussion reopened."
        pushState()
    }

    fun revealAll() {
        if (role != NetRole.HOST || phase == NetPhase.RESULT) return
        finishRound("Case file closed", "Roles revealed on request — no points awarded.", emptyMap())
    }

    fun newRound() {
        if (role != NetRole.HOST) return
        startRound()
    }

    fun returnToLobby() {
        if (role != NetRole.HOST) return
        phase = NetPhase.LOBBY
        rolesPublic = false
        pendingGuessSeat = null
        outcomeTitle = null
        outcomeDetail = null
        seats.forEach {
            it.ready = false
            it.votedFor = null
            it.alive = true
        }
        headline = "Back in the lobby."
        pushState()
    }

    // ---- Actions that work for host and client alike -------------------------

    fun markReady() {
        when (role) {
            NetRole.HOST -> {
                hostSeat()?.ready = true
                pushState()
            }
            NetRole.CLIENT -> client?.send(Proto.message(Proto.C_READY))
            NetRole.NONE -> Unit
        }
    }

    fun castVote(targetSeatId: String?) {
        when (role) {
            NetRole.HOST -> hostSeat()?.let { registerVote(it, targetSeatId) }
            NetRole.CLIENT -> {
                val msg = Proto.message(Proto.C_VOTE)
                if (targetSeatId == null) msg.put("undo", true)
                else msg.put("target", targetSeatId)
                client?.send(msg)
            }
            NetRole.NONE -> Unit
        }
    }

    fun submitGuess(word: String) {
        when (role) {
            NetRole.HOST -> hostSeat()?.let { resolveBlankGuess(it, word) }
            NetRole.CLIENT -> client?.send(Proto.message(Proto.C_GUESS).put("word", word))
            NetRole.NONE -> Unit
        }
    }

    private fun hostSeat(): Seat? = seats.firstOrNull { it.isHost }

    // =========================================================================
    // Host: handling messages from the other phones
    // =========================================================================

    private fun handleClientMessage(connId: String, msg: JSONObject) {
        when (msg.optString("t")) {
            Proto.C_JOIN -> handleJoin(connId, msg)
            Proto.C_READY -> {
                seatFor(connId)?.let { it.ready = true }
                pushState()
            }
            Proto.C_VOTE -> {
                val seat = seatFor(connId) ?: return
                if (msg.optBoolean("undo")) {
                    registerVote(seat, null)
                } else {
                    registerVote(seat, msg.optString("target"))
                }
            }
            Proto.C_GUESS -> {
                val seat = seatFor(connId) ?: return
                resolveBlankGuess(seat, msg.optString("word"))
            }
            Proto.C_LEAVE -> handleClientLeft(connId)
        }
    }

    private fun seatFor(connId: String): Seat? = seats.firstOrNull { it.connectionId == connId }

    private fun handleJoin(connId: String, msg: JSONObject) {
        val srv = server ?: return
        val requested = msg.optString("name").trim().take(GameViewModel.NAME_LIMIT)
        if (requested.isEmpty()) {
            srv.send(connId, Proto.message(Proto.S_ERROR).put("message", "Name required."))
            srv.disconnect(connId)
            return
        }

        // Someone coming back after a dropped connection keeps their seat and role.
        val returning = seats.firstOrNull {
            !it.connected && it.name.equals(requested, ignoreCase = true)
        }
        val seat: Seat
        if (returning != null) {
            returning.connectionId = connId
            returning.connected = true
            seat = returning
            headline = "${seat.name} reconnected."
        } else {
            if (phase != NetPhase.LOBBY) {
                srv.send(
                    connId,
                    Proto.message(Proto.S_ERROR)
                        .put("message", "A round is already running. Ask the host to return to the lobby.")
                )
                srv.disconnect(connId)
                return
            }
            if (seats.any { it.name.equals(requested, ignoreCase = true) }) {
                srv.send(
                    connId,
                    Proto.message(Proto.S_ERROR).put("message", "Someone already joined as $requested.")
                )
                srv.disconnect(connId)
                return
            }
            if (seats.size >= GameViewModel.MAX_PLAYERS) {
                srv.send(
                    connId,
                    Proto.message(Proto.S_ERROR).put("message", "This game is full.")
                )
                srv.disconnect(connId)
                return
            }
            seat = Seat(id = nextSeatId(), name = requested, connectionId = connId)
            seats += seat
            autoBalance()
        }

        srv.send(
            connId,
            Proto.message(Proto.S_WELCOME)
                .put("id", seat.id)
                .put("hostName", myName)
                .put("version", Proto.PROTOCOL_VERSION)
        )
        pushState()
    }

    private fun handleClientLeft(connId: String) {
        val seat = seatFor(connId) ?: return
        if (phase == NetPhase.LOBBY) {
            seats.remove(seat)
            autoBalance()
        } else {
            seat.connected = false
            seat.connectionId = null
            headline = "${seat.name} dropped out — they can rejoin with the same name."
        }
        pushState()
    }

    // ---- Voting --------------------------------------------------------------

    private fun registerVote(voter: Seat, targetId: String?) {
        if (phase != NetPhase.VOTING || pendingGuessSeat != null) return
        if (!voter.alive) return
        
        if (targetId == null) {
            voter.votedFor = null
        } else {
            val target = seats.firstOrNull { it.id == targetId && it.alive } ?: return
            voter.votedFor = target.id
        }
        pushState()
    }

    private fun votersExpected(): List<Seat> = seats.filter { it.alive && it.connected }

    fun processVotes() {
        if (role != NetRole.HOST || phase != NetPhase.VOTING || pendingGuessSeat != null) return
        val expected = votersExpected()
        if (expected.isEmpty()) return
        
        // At least one person must have voted to eliminate someone.
        val cast = expected.filter { it.votedFor != null }
        if (cast.isEmpty()) {
            headline = "No votes cast yet."
            pushState()
            return
        }

        val tally = mutableMapOf<String, Int>()
        cast.forEach { voter ->
            voter.votedFor?.let { tally[it] = (tally[it] ?: 0) + 1 }
        }
        val top = tally.maxByOrNull { it.value } ?: return
        val tied = tally.count { it.value == top.value }
        if (tied > 1) {
            seats.forEach { it.votedFor = null }
            headline = "Tied vote — everyone votes again."
            pushState()
            return
        }

        val eliminated = seats.firstOrNull { it.id == top.key } ?: return
        eliminated.alive = false
        seats.forEach { it.votedFor = null }

        if (eliminated.role == Role.BLANK) {
            pendingGuessSeat = eliminated.id
            headline = "${eliminated.name} was the blank agent — they get one guess."
            pushState()
            return
        }

        headline = "${eliminated.name} was voted out: ${roleLabel(eliminated.role)}."
        pushState()
        checkEnd()
    }

    private fun resolveBlankGuess(seat: Seat, guess: String) {
        if (pendingGuessSeat != seat.id) return
        pendingGuessSeat = null
        val correct = guess.trim().equals(civilianWord.trim(), ignoreCase = true) &&
            guess.isNotBlank()
        if (correct) {
            finishRound(
                "Blank agent wins",
                "${seat.name} was voted out, guessed \"$civilianWord\" and took the round.",
                mapOf(seat.id to BLANK_GUESS_POINTS)
            )
        } else {
            headline = if (guess.isBlank()) {
                "${seat.name} passed on the guess."
            } else {
                "${seat.name} guessed \"${guess.trim()}\" — wrong."
            }
            pushState()
            checkEnd()
        }
    }

    private fun checkEnd() {
        val civilians = seats.count { it.alive && it.role == Role.CIVILIAN }
        val agents = seats.count { it.alive && it.role != Role.CIVILIAN }
        when {
            agents == 0 -> finishRound(
                "Civilians win",
                "Every undercover agent has been caught.",
                seats.filter { it.alive && it.role == Role.CIVILIAN }
                    .associate { it.id to CIVILIAN_POINTS }
            )

            agents >= civilians -> finishRound(
                "Undercover wins",
                "The undercover side now equals or outnumbers the civilians.",
                seats.filter { it.alive && it.role != Role.CIVILIAN }
                    .associate { it.id to UNDERCOVER_POINTS }
            )

            else -> {
                phase = NetPhase.DISCUSSION
                headline = "$civilians civilians, $agents agents left. Keep talking."
                pushState()
            }
        }
    }

    private fun finishRound(title: String, detail: String, points: Map<String, Int>) {
        points.forEach { (seatId, value) ->
            seats.firstOrNull { it.id == seatId }?.let { it.points += value }
        }
        outcomeTitle = title
        outcomeDetail = detail
        rolesPublic = true
        pendingGuessSeat = null
        phase = NetPhase.RESULT
        headline = title
        pushState()
    }

    private fun roleLabel(role: Role): String = when (role) {
        Role.CIVILIAN -> "Civilian"
        Role.UNDERCOVER -> "Undercover"
        Role.BLANK -> "Blank"
    }

    // ---- State push ----------------------------------------------------------

    private fun publicViews(): List<NetPlayerView> = seats.map { seat ->
        NetPlayerView(
            id = seat.id,
            name = seat.name,
            alive = seat.alive,
            connected = seat.connected,
            ready = seat.ready,
            hasVoted = seat.votedFor != null,
            votesAgainst = seats.count { it.votedFor == seat.id },
            revealedRole = if (rolesPublic || !seat.alive) roleLabel(seat.role) else null,
            isHost = seat.isHost,
            points = seat.points
        )
    }

    private fun stateFor(seat: Seat?): NetState {
        var views = publicViews()
        if (phase != NetPhase.LOBBY) {
            // Sort players by speaking order for a consistent sequence on every screen.
            views = speakingOrder.mapNotNull { id -> views.find { it.id == id } }
        }

        return NetState(
            phase = phase,
            round = roundNumber,
            hostName = myName,
            players = views,
            order = if (phase == NetPhase.LOBBY) emptyList() else speakingOrder.map { id -> seats.find { it.id == id }?.name ?: "" },
            message = headline,
            outcomeTitle = outcomeTitle,
            outcomeDetail = outcomeDetail,
            civilianWord = if (rolesPublic) civilianWord else null,
            undercoverWord = if (rolesPublic) undercoverWord else null,
            votesCast = votersExpected().count { it.votedFor != null },
            votesNeeded = votersExpected().size,
            undercoverCount = undercoverCount,
            useBlank = useBlank,
            timerSeconds = timerSeconds,
            self = seat?.let {
                NetSelf(
                    id = it.id,
                    name = it.name,
                    role = roleLabel(it.role),
                    word = it.word,
                    alive = it.alive,
                    ready = it.ready,
                    votedFor = it.votedFor,
                    needsGuess = pendingGuessSeat == it.id,
                    isHost = it.isHost
                )
            }
        )
    }

    private fun pushState() {
        if (role != NetRole.HOST) return
        netState = stateFor(hostSeat())
        val srv = server ?: return
        srv.sendEach { connId ->
            val seat = seats.firstOrNull { it.connectionId == connId } ?: return@sendEach null
            stateFor(seat).toJson()
        }
    }

    // =========================================================================
    // Joining
    // =========================================================================

    fun startDiscovery() {
        discoveredHosts = emptyList()
        discovery.startDiscovery(
            onFound = { host ->
                if (discoveredHosts.none { it.address == host.address && it.port == host.port }) {
                    discoveredHosts = discoveredHosts + host
                }
            },
            onError = { message -> status = message }
        )
    }

    fun stopDiscovery() {
        discovery.stopDiscovery()
    }

    fun joinGame(name: String, address: String, port: Int = Proto.PORT) {
        val trimmed = name.trim().take(GameViewModel.NAME_LIMIT)
        if (trimmed.isEmpty()) {
            status = "Enter your name first."
            return
        }
        if (address.isBlank()) {
            status = "Pick a game or type the host's IP."
            return
        }
        myName = trimmed
        status = "Connecting to $address…"

        val c = GameClient(
            onMessage = { msg -> handleServerMessage(msg) },
            onConnected = {
                connected = true
                role = NetRole.CLIENT
                status = null
            },
            onClosed = { reason ->
                connected = false
                if (role == NetRole.CLIENT) {
                    status = reason ?: "Disconnected from the host."
                    role = NetRole.NONE
                    netState = NetState()
                }
                client = null
            }
        )
        client = c
        stopDiscovery()
        c.connect(address, port, Proto.message(Proto.C_JOIN).put("name", trimmed))
    }

    fun leaveGame() {
        client?.disconnect()
        client = null
        connected = false
        role = NetRole.NONE
        netState = NetState()
    }

    private fun handleServerMessage(msg: JSONObject) {
        when (msg.optString("t")) {
            Proto.S_WELCOME -> status = null
            Proto.S_STATE -> {
                netState = NetState.fromJson(msg)
                status = null
            }
            Proto.S_ERROR -> {
                status = msg.optString("message")
                leaveGame()
            }
            Proto.S_KICKED -> {
                status = msg.optString("reason", "The host ended the game.")
                leaveGame()
            }
        }
    }

    fun clearStatus() {
        status = null
    }

    override fun onCleared() {
        super.onCleared()
        discovery.shutdown()
        server?.stop()
        client?.disconnect()
    }

    companion object {
        const val CIVILIAN_POINTS = 2
        const val UNDERCOVER_POINTS = 3
        const val BLANK_GUESS_POINTS = 4
    }
}
