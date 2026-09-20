package com.faizan.undercover.net

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

/** The joining phone's end of the connection. */
class GameClient(
    private val onMessage: (JSONObject) -> Unit,
    private val onConnected: () -> Unit,
    private val onClosed: (String?) -> Unit
) {

    private val main = Handler(Looper.getMainLooper())
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    @Volatile private var running = false

    fun connect(host: String, port: Int, joinPayload: JSONObject) {
        if (running) return
        running = true
        Thread({
            try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 8000)
                s.tcpNoDelay = true
                socket = s
                writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
                send(joinPayload)
                main.post { onConnected() }

                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                while (running && !s.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val obj = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    main.post { onMessage(obj) }
                }
                finish(null)
            } catch (e: Exception) {
                finish(friendlyError(e))
            }
        }, "undercover-client").start()
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is java.net.SocketTimeoutException -> "Could not reach the host. Check you're on the same Wi-Fi."
        is java.net.ConnectException -> "The host isn't accepting connections yet."
        is java.net.UnknownHostException -> "That address doesn't look right."
        else -> e.localizedMessage ?: "Connection lost."
    }

    fun send(payload: JSONObject) {
        val w = writer ?: return
        Thread {
            try {
                synchronized(w) {
                    w.write(payload.toString())
                    w.write("\n")
                    w.flush()
                }
            } catch (_: Exception) {
                finish("Connection lost.")
            }
        }.start()
    }

    fun disconnect() {
        if (!running) return
        runCatching { send(Proto.message(Proto.C_LEAVE)) }
        finish(null)
    }

    private fun finish(reason: String?) {
        if (!running) return
        running = false
        runCatching { socket?.close() }
        socket = null
        writer = null
        main.post { onClosed(reason) }
    }
}
