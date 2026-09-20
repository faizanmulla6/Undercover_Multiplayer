package com.faizan.undercover.net

import org.json.JSONArray
import org.json.JSONObject

/**
 * Wire protocol for LAN multiplayer.
 *
 * One TCP connection per phone, newline-delimited JSON. The host is the only
 * authority: it deals roles, counts votes and decides outcomes. Every client
 * simply renders the last [NetState] it was handed, so there is no way for a
 * client to learn another player's word — the host never sends it.
 */
object Proto {

    const val PORT = 45987
    const val SERVICE_TYPE = "_undercover._tcp"
    const val PROTOCOL_VERSION = 1

    // client -> host
    const val C_JOIN = "join"
    const val C_READY = "ready"
    const val C_VOTE = "vote"
    const val C_GUESS = "guess"
    const val C_LEAVE = "leave"

    // host -> client
    const val S_WELCOME = "welcome"
    const val S_STATE = "state"
    const val S_ERROR = "error"
    const val S_KICKED = "kicked"

    fun message(type: String): JSONObject = JSONObject().put("t", type)
}

enum class NetPhase { LOBBY, REVEAL, DISCUSSION, VOTING, RESULT }

/** What every phone is allowed to know about every other player. */
data class NetPlayerView(
    val id: String,
    val name: String,
    val alive: Boolean,
    val connected: Boolean,
    val ready: Boolean,
    val hasVoted: Boolean,
    val votesAgainst: Int,
    /** Only filled in once the role is public (eliminated, or round over). */
    val revealedRole: String?,
    val isHost: Boolean,
    val points: Int
)

/** The private part of the state — different for every phone. */
data class NetSelf(
    val id: String,
    val name: String,
    val role: String,
    val word: String,
    val alive: Boolean,
    val ready: Boolean,
    val votedFor: String?,
    val needsGuess: Boolean,
    val isHost: Boolean
)

data class NetState(
    val phase: NetPhase = NetPhase.LOBBY,
    val round: Int = 0,
    val hostName: String = "",
    val players: List<NetPlayerView> = emptyList(),
    val order: List<String> = emptyList(),
    val message: String = "",
    val outcomeTitle: String? = null,
    val outcomeDetail: String? = null,
    val civilianWord: String? = null,
    val undercoverWord: String? = null,
    val votesCast: Int = 0,
    val votesNeeded: Int = 0,
    val undercoverCount: Int = 1,
    val useBlank: Boolean = false,
    val timerSeconds: Int = 0,
    val self: NetSelf? = null
) {
    val alivePlayers: List<NetPlayerView> get() = players.filter { it.alive }
    val readyCount: Int get() = players.count { it.ready }

    fun toJson(): JSONObject {
        val obj = Proto.message(Proto.S_STATE)
        obj.put("phase", phase.name)
        obj.put("round", round)
        obj.put("hostName", hostName)
        obj.put("message", message)
        obj.put("votesCast", votesCast)
        obj.put("votesNeeded", votesNeeded)
        obj.put("undercoverCount", undercoverCount)
        obj.put("useBlank", useBlank)
        obj.put("timerSeconds", timerSeconds)
        outcomeTitle?.let { obj.put("outcomeTitle", it) }
        outcomeDetail?.let { obj.put("outcomeDetail", it) }
        civilianWord?.let { obj.put("civilianWord", it) }
        undercoverWord?.let { obj.put("undercoverWord", it) }

        val arr = JSONArray()
        players.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("alive", p.alive)
            o.put("connected", p.connected)
            o.put("ready", p.ready)
            o.put("hasVoted", p.hasVoted)
            o.put("votesAgainst", p.votesAgainst)
            o.put("isHost", p.isHost)
            o.put("points", p.points)
            p.revealedRole?.let { o.put("revealedRole", it) }
            arr.put(o)
        }
        obj.put("players", arr)
        obj.put("order", JSONArray(order))

        self?.let { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("role", s.role)
            o.put("word", s.word)
            o.put("alive", s.alive)
            o.put("ready", s.ready)
            o.put("needsGuess", s.needsGuess)
            o.put("isHost", s.isHost)
            s.votedFor?.let { v -> o.put("votedFor", v) }
            obj.put("self", o)
        }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): NetState {
            val players = mutableListOf<NetPlayerView>()
            val arr = obj.optJSONArray("players") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                players += NetPlayerView(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    alive = o.optBoolean("alive", true),
                    connected = o.optBoolean("connected", true),
                    ready = o.optBoolean("ready", false),
                    hasVoted = o.optBoolean("hasVoted", false),
                    votesAgainst = o.optInt("votesAgainst", 0),
                    revealedRole = if (o.has("revealedRole")) o.optString("revealedRole") else null,
                    isHost = o.optBoolean("isHost", false),
                    points = o.optInt("points", 0)
                )
            }

            val order = mutableListOf<String>()
            val orderArr = obj.optJSONArray("order") ?: JSONArray()
            for (i in 0 until orderArr.length()) order += orderArr.optString(i)

            val selfObj = obj.optJSONObject("self")
            val self = selfObj?.let {
                NetSelf(
                    id = it.optString("id"),
                    name = it.optString("name"),
                    role = it.optString("role"),
                    word = it.optString("word"),
                    alive = it.optBoolean("alive", true),
                    ready = it.optBoolean("ready", false),
                    votedFor = if (it.has("votedFor")) it.optString("votedFor") else null,
                    needsGuess = it.optBoolean("needsGuess", false),
                    isHost = it.optBoolean("isHost", false)
                )
            }

            val phase = runCatching { NetPhase.valueOf(obj.optString("phase")) }
                .getOrDefault(NetPhase.LOBBY)

            return NetState(
                phase = phase,
                round = obj.optInt("round", 0),
                hostName = obj.optString("hostName"),
                players = players,
                order = order,
                message = obj.optString("message"),
                outcomeTitle = if (obj.has("outcomeTitle")) obj.optString("outcomeTitle") else null,
                outcomeDetail = if (obj.has("outcomeDetail")) obj.optString("outcomeDetail") else null,
                civilianWord = if (obj.has("civilianWord")) obj.optString("civilianWord") else null,
                undercoverWord = if (obj.has("undercoverWord")) obj.optString("undercoverWord") else null,
                votesCast = obj.optInt("votesCast", 0),
                votesNeeded = obj.optInt("votesNeeded", 0),
                undercoverCount = obj.optInt("undercoverCount", 1),
                useBlank = obj.optBoolean("useBlank", false),
                timerSeconds = obj.optInt("timerSeconds", 0),
                self = self
            )
        }
    }
}
