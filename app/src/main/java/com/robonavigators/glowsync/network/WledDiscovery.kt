package com.robonavigators.glowsync.network

import android.content.Context
import android.net.wifi.WifiManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import org.json.JSONObject

data class WledDevice(val name: String, val host: String, val ledCount: Int, val version: String)
/** mDNS results are candidates only; the WLED JSON API verifies each one. */
class WledDiscovery(context: Context, private val onDevice: (WledDevice) -> Unit, private val onError: (String) -> Unit) : AutoCloseable {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val multicast = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.createMulticastLock("GlowSync discovery")?.apply { setReferenceCounted(false) }
    private val executor = ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,ArrayBlockingQueue(32),ThreadPoolExecutor.DiscardPolicy())
    @Volatile private var running = false
    private val queue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(type: String) { }
        override fun onDiscoveryStopped(type: String) { }
        override fun onStartDiscoveryFailed(type: String, code: Int) { running=false; if (multicast?.isHeld == true) multicast.release(); onError("Discovery failed ($code); enter an address manually") }
        override fun onStopDiscoveryFailed(type: String, code: Int) { }
        override fun onServiceLost(service: NsdServiceInfo) { }
        override fun onServiceFound(service: NsdServiceInfo) {
            synchronized(queue) { if (!running || queue.size >= 32) return; if(queue.none { it.serviceName==service.serviceName && it.serviceType==service.serviceType }) queue.addLast(service) }
            resolveNext()
        }
    }
    private fun resolveNext() {
        val service = synchronized(queue) {
            if (!running || resolving || queue.isEmpty()) return
            resolving=true; queue.removeFirst()
        }
        try {
            @Suppress("DEPRECATION")
            nsd.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(s: NsdServiceInfo, code: Int) { finishResolve() }
                @Suppress("DEPRECATION")
                override fun onServiceResolved(s: NsdServiceInfo) {
                    val host=s.host?.hostAddress
                    if (host != null && running) try { executor.execute { try { val result=verify(host); if(running) onDevice(result) } catch (_: Exception) { } } } catch (_: java.util.concurrent.RejectedExecutionException) { }
                    finishResolve()
                }
            })
        } catch (_: Exception) { finishResolve() }
    }
    private fun finishResolve() { synchronized(queue) { resolving=false }; resolveNext() }

    fun start() { if (!running) { running=true; try { multicast?.acquire(); nsd.discoverServices("_http._tcp.",NsdManager.PROTOCOL_DNS_SD,listener) } catch (e: Exception) { running=false; if (multicast?.isHeld == true) multicast.release(); onError("Discovery unavailable: ${e.javaClass.simpleName}") } } }
    override fun close() { if (running) try { nsd.stopServiceDiscovery(listener) } catch (_: Exception) { }; running=false; synchronized(queue) { queue.clear() }; if (multicast?.isHeld == true) multicast.release(); executor.shutdownNow() }
    companion object {
        fun verify(host: String): WledDevice {
            require(host.isNotBlank() && !host.contains('/') && !host.contains('@') && !host.contains(':'))
            val c=URL("http://$host/json/info").openConnection() as HttpURLConnection
            c.connectTimeout=2500; c.readTimeout=2500; c.instanceFollowRedirects=false
            try {
                val json=JSONObject(c.inputStream.bufferedReader().use { reader ->
                    val text=StringBuilder(); val chunk=CharArray(4096)
                    while(true) { val n=reader.read(chunk); if(n<0) break; require(text.length+n<=65536) { "WLED response too large" }; text.append(chunk,0,n) }
                    text.toString()
                })
                require(json.has("ver") && json.optJSONObject("leds") != null) { "Not a WLED info response" }
                return WledDevice(json.optString("name","WLED"),host,json.getJSONObject("leds").optInt("count"),json.optString("ver"))
            } finally { c.disconnect() }
        }
    }
}
