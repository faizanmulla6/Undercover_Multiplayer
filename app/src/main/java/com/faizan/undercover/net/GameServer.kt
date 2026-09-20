package com.faizan.undercover.net

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Accepts connections from the other phones on the Wi-Fi network. One thread per
 * client, newline-delimited JSON. Every callback is posted to the main thread so
 * the ViewModel can touch Compose state directly.
 */
class GameServer(
    private val onMessage: (connectionId: String, JSONObject) -> Unit,
    private val onJoined: (connectionId: String) -> Unit,
    private val onLeft: (connectionId: String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val main = Handler(Looper.getMainLooper())
    private val connections = Collections.synchronizedMap(LinkedHashMap<String, Connection>())
    private val counter = AtomicInteger(0)

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var running = false

    var port: Int = Proto.PORT
        private set

    fun start(): Boolean {
        if (running) return true
        return try {
            // Fall back to a nearby port if something else already has the default.
            val socket = openSocket()
            serverSocket = socket
            port = socket.localPort
            running = true
            Thread({ acceptLoop(socket) }, "undercover-accept").start()
            true
        } catch (e: Exception) {
            main.post { onError(e.localizedMessage ?: "Could not start the game server.") }
            false
        }
    }

    private fun openSocket(): ServerSocket {
        var lastError: Exception? = null
        for (candidate in Proto.PORT..(Proto.PORT + 8)) {
            try {
                return ServerSocket(candidate)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No free port")
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running) {
            try {
                val client = socket.accept()
                client.tcpNoDelay = true
                val id = "c" + counter.incrementAndGet()
                val connection = Connection(id, client)
                connections[id] = connection
                connection.start()
                main.post { onJoined(id) }
            } catch (e: Exception) {
                if (running) main.post { onError("Lost a connection: ${e.localizedMessage}") }
                if (socket.isClosed) break
            }
        }
    }

    fun send(connectionId: String, payload: JSONObject) {
        connections[connectionId]?.send(payload)
    }

    fun broadcast(payload: JSONObject) {
        val snapshot = synchronized(connections) { connections.values.toList() }
        snapshot.forEach { it.send(payload) }
    }

    /** Lets the caller send a different payload to every phone (private words). */
    fun sendEach(builder: (connectionId: String) -> JSONObject?) {
        val snapshot = synchronized(connections) { connections.values.toList() }
        snapshot.forEach { conn -> builder(conn.id)?.let { conn.send(it) } }
    }

    fun disconnect(connectionId: String) {
        connections.remove(connectionId)?.close()
    }

    fun stop() {
        running = false
        val snapshot = synchronized(connections) { connections.values.toList() }
        snapshot.forEach { it.close() }
        connections.clear()
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private inner class Connection(val id: String, private val socket: Socket) {

        private val writer: BufferedWriter =
            BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
        private var thread: Thread? = null

        fun start() {
            thread = Thread({ readLoop() }, "undercover-conn-$id").also { it.start() }
        }

        private fun readLoop() {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                while (running && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val obj = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    main.post { onMessage(id, obj) }
                }
            } catch (_: Exception) {
                // Dropping out here simply means this phone went away.
            } finally {
                connections.remove(id)
                close()
                main.post { onLeft(id) }
            }
        }

        fun send(payload: JSONObject) {
            Thread {
                try {
                    synchronized(writer) {
                        writer.write(payload.toString())
                        writer.write("\n")
                        writer.flush()
                    }
                } catch (_: Exception) {
                    close()
                }
            }.start()
        }

        fun close() {
            runCatching { socket.close() }
        }
    }

    companion object {
        /** The phone's address on the Wi-Fi network, for the "type this in" fallback. */
        fun localIpAddress(): String? {
            return try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
                Collections.list(interfaces).asSequence()
                    .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
                    .flatMap { Collections.list(it.inetAddresses).asSequence() }
                    .filterIsInstance<InetAddress>()
                    .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains('.') == true }
                    ?.hostAddress
            } catch (_: Exception) {
                null
            }
        }
    }
}
