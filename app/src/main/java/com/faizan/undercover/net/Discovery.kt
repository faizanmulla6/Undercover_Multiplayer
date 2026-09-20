package com.faizan.undercover.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper

data class DiscoveredHost(val label: String, val address: String, val port: Int)

/**
 * Advertises the host game over mDNS and finds it again on the joining phones,
 * so nobody has to type an IP address. Typing one still works as a fallback
 * because some home routers block multicast.
 */
class Discovery(context: Context) {

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifi = appContext.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val main = Handler(Looper.getMainLooper())

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // ---------------- host side ----------------

    fun register(serviceName: String, port: Int, onError: (String) -> Unit = {}) {
        val manager = nsd ?: return
        unregister()
        val info = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = Proto.SERVICE_TYPE
            this.port = port
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                main.post { onError("Couldn't advertise the game (code $errorCode). Players can still join by IP.") }
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        runCatching {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { onError("Couldn't advertise the game. Players can still join by IP.") }
    }

    fun unregister() {
        val manager = nsd ?: return
        registrationListener?.let { runCatching { manager.unregisterService(it) } }
        registrationListener = null
    }

    // ---------------- joining side ----------------

    fun startDiscovery(onFound: (DiscoveredHost) -> Unit, onError: (String) -> Unit = {}) {
        val manager = nsd ?: run {
            onError("This device can't search the network. Enter the host's IP instead.")
            return
        }
        stopDiscovery()
        acquireMulticastLock()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType?.contains("undercover") != true) return
                resolve(manager, service, onFound)
            }

            override fun onServiceLost(service: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { manager.stopServiceDiscovery(this) }
                main.post { onError("Network search unavailable. Enter the host's IP instead.") }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { manager.stopServiceDiscovery(this) }
            }
        }
        discoveryListener = listener
        runCatching {
            manager.discoverServices(Proto.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { onError("Network search unavailable. Enter the host's IP instead.") }
    }

    @Suppress("DEPRECATION")
    private fun resolve(
        manager: NsdManager,
        service: NsdServiceInfo,
        onFound: (DiscoveredHost) -> Unit
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceResolved(info: NsdServiceInfo) {
                val address = info.host?.hostAddress ?: return
                main.post {
                    onFound(
                        DiscoveredHost(
                            label = info.serviceName ?: "Undercover game",
                            address = address,
                            port = info.port
                        )
                    )
                }
            }
        }
        runCatching { manager.resolveService(service, resolveListener) }
    }

    fun stopDiscovery() {
        val manager = nsd ?: return
        discoveryListener?.let { runCatching { manager.stopServiceDiscovery(it) } }
        discoveryListener = null
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        if (multicastLock != null) return
        multicastLock = runCatching {
            wifi?.createMulticastLock("undercover-nsd")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        }.getOrNull()
    }

    private fun releaseMulticastLock() {
        runCatching { multicastLock?.takeIf { it.isHeld }?.release() }
        multicastLock = null
    }

    fun shutdown() {
        unregister()
        stopDiscovery()
    }
}
