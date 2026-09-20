package com.faizan.undercover.data

import android.content.Context
import com.faizan.undercover.model.VoteStyle
import com.faizan.undercover.model.WordMode
import com.faizan.undercover.model.WordPair
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything is on-device. A single SharedPreferences file with small JSON blobs
 * keeps the app dependency-free and instant to read on launch.
 */
class GameStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("undercover_prefs", Context.MODE_PRIVATE)

    // ----- Players -----

    fun loadPlayers(): List<String> = readStringArray(KEY_PLAYERS)

    fun savePlayers(names: List<String>) {
        prefs.edit().putString(KEY_PLAYERS, JSONArray(names).toString()).apply()
    }

    // ----- Scores -----

    fun loadScores(): Map<String, Int> {
        val raw = prefs.getString(KEY_SCORES, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key -> put(key, obj.optInt(key, 0)) }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveScores(scores: Map<String, Int>) {
        val obj = JSONObject()
        scores.forEach { (name, points) -> obj.put(name, points) }
        prefs.edit().putString(KEY_SCORES, obj.toString()).apply()
    }

    // ----- Custom word pairs -----

    fun loadCustomPairs(): List<WordPair> {
        val raw = prefs.getString(KEY_PAIRS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val item = arr.optJSONArray(i) ?: return@mapNotNull null
                val a = item.optString(0).trim()
                val b = item.optString(1).trim()
                if (a.isEmpty() || b.isEmpty()) null else WordPair(a, b)
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustomPairs(pairs: List<WordPair>) {
        val arr = JSONArray()
        pairs.forEach { arr.put(JSONArray(listOf(it.civilian, it.undercover))) }
        prefs.edit().putString(KEY_PAIRS, arr.toString()).apply()
    }

    // ----- Settings -----

    fun loadWordMode(): WordMode =
        runCatching { WordMode.valueOf(prefs.getString(KEY_MODE, null) ?: "") }
            .getOrDefault(WordMode.BUILTIN)

    fun saveWordMode(mode: WordMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun loadDisabledCategories(): Set<String> = readStringArray(KEY_DISABLED_CATS).toSet()

    fun saveDisabledCategories(categories: Set<String>) {
        prefs.edit().putString(KEY_DISABLED_CATS, JSONArray(categories.toList()).toString()).apply()
    }

    fun loadUndercoverCount(): Int = prefs.getInt(KEY_UNDERCOVER, 1)

    fun saveUndercoverCount(value: Int) {
        prefs.edit().putInt(KEY_UNDERCOVER, value).apply()
    }

    fun loadUseBlank(): Boolean = prefs.getBoolean(KEY_BLANK, false)

    fun saveUseBlank(value: Boolean) {
        prefs.edit().putBoolean(KEY_BLANK, value).apply()
    }

    fun loadTimerSeconds(): Int = prefs.getInt(KEY_TIMER, 0)

    fun saveTimerSeconds(value: Int) {
        prefs.edit().putInt(KEY_TIMER, value).apply()
    }

    fun loadVoteStyle(): VoteStyle =
        runCatching { VoteStyle.valueOf(prefs.getString(KEY_VOTE_STYLE, null) ?: "") }
            .getOrDefault(VoteStyle.QUICK)

    fun saveVoteStyle(style: VoteStyle) {
        prefs.edit().putString(KEY_VOTE_STYLE, style.name).apply()
    }

    fun loadSyncUrl(): String = prefs.getString(KEY_SYNC_URL, "") ?: ""

    fun saveSyncUrl(url: String) {
        prefs.edit().putString(KEY_SYNC_URL, url).apply()
    }

    fun loadDarkMode(): Boolean? {
        if (!prefs.contains(KEY_DARK_MODE)) return null
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun saveDarkMode(dark: Boolean?) {
        if (dark == null) {
            prefs.edit().remove(KEY_DARK_MODE).apply()
        } else {
            prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
        }
    }

    private fun readStringArray(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_PLAYERS = "players"
        const val KEY_SCORES = "scores"
        const val KEY_PAIRS = "custom_pairs"
        const val KEY_MODE = "word_mode"
        const val KEY_DISABLED_CATS = "disabled_categories"
        const val KEY_UNDERCOVER = "undercover_count"
        const val KEY_BLANK = "use_blank"
        const val KEY_TIMER = "timer_seconds"
        const val KEY_VOTE_STYLE = "vote_style"
        const val KEY_SYNC_URL = "sync_url"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
