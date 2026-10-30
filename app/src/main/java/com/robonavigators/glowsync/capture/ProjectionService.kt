package com.robonavigators.glowsync.capture

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.net.ConnectivityManager
import android.net.Network
import com.robonavigators.glowsync.MainActivity
import com.robonavigators.glowsync.network.DdpSender
import com.robonavigators.glowsync.network.WledDiscovery
import com.robonavigators.glowsync.processing.EdgeSampler
import com.robonavigators.glowsync.processing.Layout
import com.robonavigators.glowsync.processing.Smoother
import com.robonavigators.glowsync.processing.BlackBarDetector
import com.robonavigators.glowsync.processing.ColorAdjust
import com.robonavigators.glowsync.processing.DarkFrameFallback
import com.robonavigators.glowsync.performance.AdaptiveRate
import com.robonavigators.glowsync.performance.ResolutionPolicy
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicLong

object LiveStatus { @Volatile var text = "Capture inactive"; @Volatile var analyzed = 0L; private val droppedCounter = AtomicLong(); val dropped get() = droppedCounter.get(); fun drop() { droppedCounter.incrementAndGet() }; @Volatile var processingMs = 0.0; @Volatile var sent = 0L; @Volatile var cap = 25; @Volatile var sendMs = 0.0; @Volatile var active = false; @Volatile var preview: ByteArray? = null; @Volatile var captureFps = 0.0; @Volatile var callbackFps=0.0; @Volatile var outputFps = 0.0; @Volatile var encodeMs=0.0; @Volatile var frameAgeMs=0.0; @Volatile var controller="Not checked" }
class ProjectionService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var sender: DdpSender? = null
    private val network = Executors.newSingleThreadExecutor()
    private val control = Executors.newSingleThreadScheduledExecutor()
    private var controlDelay=2L
    private data class LedFrame(val rgb:ByteArray,val sampledAtNanos:Long)
    private val pending = AtomicReference<LedFrame?>(null)
    private val sending = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastFrame = 0L
    private var lastCallbackMs = 0L
    private var windowStart = 0L
    private var analyzedAtWindow = 0L
    private var callbacks=0L
    private var callbacksAtWindow=0L
    private var sentAtWindow = 0L
    private var smoother = Smoother(80f)
    private val bars = BlackBarDetector()
    private var brightness = 1f
    private var saturation = 1f
    private var depth = .1f
    private var detectBars = false
    private var weighted = false
    private var sideGain=FloatArray(12) { 1f }
    private var whiteRed=1f; private var whiteGreen=1f; private var whiteBlue=1f
    private var smoothingSetting = -1
    private var fpsSetting = -1
    private var fallbackSetting = ""
    private var fallback = DarkFrameFallback(DarkFrameFallback.Mode.PASS_THROUGH)
    private var prefs: android.content.SharedPreferences? = null
    private val reloadSettings = Runnable { if(!closed) loadSettings() }
    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> captureHandler?.let { it.removeCallbacks(reloadSettings); it.postDelayed(reloadSettings,150) } }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var sleepRegistered=false
    private val sleepReceiver=object: BroadcastReceiver() { override fun onReceive(context:Context?, intent:Intent?) { if(intent?.action==Intent.ACTION_SCREEN_OFF) { LiveStatus.text="Display went to sleep; start sync again with fresh consent"; stopSelf() } } }
    @Volatile private var retryAt = 0L
    private var retryDelay = 1000L
    private var rate = AdaptiveRate(25)
    private var resolution = ResolutionPolicy(25,0)
    private var contentAspect = 9f/16f
    private var resizePending=false
    @Volatile private var closed = false
    private var captureHandler: Handler? = null
    private var activeHost = ""
    private var activeLayout = Layout(30,18,30,18)
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() { LiveStatus.text = "Capture ended; permission is required again"; stopSelf() }
        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width <= 0 || height <= 0 || display == null || closed) return
            contentAspect=height.toFloat()/width
            reconfigure(resolution.width)
        }
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        if (projection != null) return START_NOT_STICKY
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("consent", Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra<Intent>("consent")
        val host = intent?.getStringExtra("host").orEmpty()
        if (data == null || host.isBlank()) { stopSelf(); return START_NOT_STICKY }
        try {
            val channel = NotificationChannel("capture", "Screen synchronization", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            val stop = PendingIntent.getService(this, 1, Intent(this, javaClass).setAction("stop"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(this,"capture").setContentTitle("GlowSync is active").setContentText("Screen colours are processed locally").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(open).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1,notification)
            worker = HandlerThread("GlowSync capture").also { it.start() }
            val handler = Handler(worker!!.looper); captureHandler=handler; activeHost=host
            val p = getSystemService(MediaProjectionManager::class.java).getMediaProjection(intent!!.getIntExtra("result", Activity.RESULT_CANCELED),data)
            projection = p; p.registerCallback(callback,handler)
            val metrics = resources.displayMetrics
            val requestedFps=getSharedPreferences("glowsync",MODE_PRIVATE).getInt("fps",25).coerceIn(10,60)
            resolution=ResolutionPolicy(requestedFps,SystemClock.elapsedRealtime())
            contentAspect=metrics.heightPixels.toFloat()/metrics.widthPixels
            val width = resolution.width; val height = (width * contentAspect).toInt().coerceIn(64,480)
            val ir = ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,3); reader = ir
            prefs = getSharedPreferences("glowsync", MODE_PRIVATE)
            loadSettings()
            prefs?.registerOnSharedPreferenceChangeListener(preferenceListener)
            val manager=getSystemService(ConnectivityManager::class.java)
            networkCallback=object: ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { resetNetwork("Network available; UDP delivery is unconfirmed") }
                override fun onLost(network: Network) { resetNetwork("Network changed or disconnected") }
            }
            manager.registerDefaultNetworkCallback(networkCallback!!)
            if(Build.VERSION.SDK_INT>=33) registerReceiver(sleepReceiver,IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED)
            else @Suppress("DEPRECATION") registerReceiver(sleepReceiver,IntentFilter(Intent.ACTION_SCREEN_OFF))
            sleepRegistered=true
            checkController(host)
            attachReader(ir,handler)
            lastCallbackMs=SystemClock.elapsedRealtime()
            val watchdog=object: Runnable { override fun run() {
                if(closed) return
                if(SystemClock.elapsedRealtime()-lastCallbackMs > 3000) { LiveStatus.captureFps=0.0; LiveStatus.callbackFps=0.0; LiveStatus.outputFps=0.0; LiveStatus.text="No new capture frames; the source may be paused or unavailable" }
                handler.postDelayed(this,2000)
            } }
            handler.postDelayed(watchdog,2000)
            display = p.createVirtualDisplay("GlowSync",width,height,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,ir.surface,null,handler)
            LiveStatus.active=true
            LiveStatus.text = "Capture active; UDP sends are unacknowledged"
        } catch (e: Exception) { LiveStatus.text = "Capture could not start: ${e.javaClass.simpleName}"; stopSelf() }
        return START_NOT_STICKY
    }
    private fun loadSettings() {
            val prefs=prefs ?: return
            brightness=prefs.getFloat("brightness",1f).coerceIn(0f,2f)
            saturation=prefs.getFloat("saturation",1f).coerceIn(0f,2f)
            depth=prefs.getFloat("depth",.1f).coerceIn(.01f,.5f)
            detectBars=prefs.getBoolean("bars",false)
            weighted=prefs.getBoolean("weighted",false)
            sideGain=FloatArray(12) { prefs.getFloat("sideGain$it",1f).coerceIn(0f,2f) }
            whiteRed=prefs.getFloat("whiteRed",1f).coerceIn(0f,2f); whiteGreen=prefs.getFloat("whiteGreen",1f).coerceIn(0f,2f); whiteBlue=prefs.getFloat("whiteBlue",1f).coerceIn(0f,2f)
            val smoothingValue=prefs.getInt("smoothing",80).coerceIn(0,1000)
            if(smoothingValue != smoothingSetting) { smoother=Smoother(smoothingValue.toFloat()); smoothingSetting=smoothingValue }
            val fpsValue=prefs.getInt("fps",25).coerceIn(10,60)
            if(fpsValue != fpsSetting) { rate=AdaptiveRate(fpsValue); fpsSetting=fpsValue; resolution=ResolutionPolicy(fpsValue,SystemClock.elapsedRealtime()); if(display != null) reconfigure(resolution.width) }
            LiveStatus.cap = rate.fps
            val layout = Layout(prefs.getInt("top",30),prefs.getInt("right",18),prefs.getInt("bottom",30),prefs.getInt("left",18),prefs.getInt("offset",0),prefs.getBoolean("reverse",false))
            activeLayout=layout
            val mode=runCatching { DarkFrameFallback.Mode.valueOf(prefs.getString("fallback","PASS_THROUGH") ?: "PASS_THROUGH") }.getOrDefault(DarkFrameFallback.Mode.PASS_THROUGH)
            if (mode.name != fallbackSetting) { fallback=DarkFrameFallback(mode); fallbackSetting=mode.name }
    }
    private fun reconfigure(width: Int) {
        if(closed || display==null) return
        val handler=captureHandler ?: return
        val height=(width*contentAspect).toInt().coerceIn(64,480)
        val replacement=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,3)
        attachReader(replacement,handler)
        val old=reader
        try {
            display?.resize(width,height,resources.displayMetrics.densityDpi)
            display?.surface=replacement.surface
            reader=replacement; bars.reset()
            old?.setOnImageAvailableListener(null,null); old?.close()
        } catch(e:Exception) { replacement.close(); LiveStatus.text="Capture resize failed: ${e.javaClass.simpleName}" }
    }
    private fun checkController(host:String) {
        if(closed) return
        try { control.execute {
            if(closed) return@execute
            val next=try { val device=WledDiscovery.verify(host); if(!closed) LiveStatus.controller="HTTP verified: ${device.name}, ${device.ledCount} LEDs"; controlDelay=2; 30L }
                catch(_:Exception) { if(!closed) LiveStatus.controller="HTTP unavailable; UDP delivery unknown"; val delay=controlDelay; controlDelay=(controlDelay*2).coerceAtMost(30); delay }
            if(!closed) try { control.schedule({ checkController(host) },next,TimeUnit.SECONDS) } catch (_: java.util.concurrent.RejectedExecutionException) { }
        } } catch (_: java.util.concurrent.RejectedExecutionException) { }
    }
    private fun resetNetwork(message: String) {
        if (closed) return
        try { network.execute { sender?.close(); sender=null; retryAt=0; retryDelay=1000; LiveStatus.text=message } } catch (_: java.util.concurrent.RejectedExecutionException) { }
    }
    private fun attachReader(ir: ImageReader, handler: Handler) {
            ir.setOnImageAvailableListener({ source ->
                val image = try { source.acquireLatestImage() } catch (_: Exception) { null } ?: return@setOnImageAvailableListener
                try {
                    if(closed) return@setOnImageAvailableListener
                    val now = SystemClock.elapsedRealtimeNanos()
                    lastCallbackMs=SystemClock.elapsedRealtime()
                    if(LiveStatus.text.startsWith("No new capture frames")) LiveStatus.text="Capture active; UDP sends are unacknowledged"
                    callbacks++
                    if (now-lastFrame < 1_000_000_000L / rate.fps) { LiveStatus.drop(); return@setOnImageAvailableListener }
                    val started = now; val plane = image.planes[0]
                    val bounds = if (detectBars) bars.detect(plane.buffer,image.width,image.height,plane.rowStride,plane.pixelStride) else com.robonavigators.glowsync.processing.ContentBounds(0,0,image.width,image.height)
                    val rgb = EdgeSampler.sample(plane.buffer,image.width,image.height,plane.rowStride,plane.pixelStride,activeLayout,depth,bounds,weighted,sideGain)
                    val fallbackRgb=fallback.apply(rgb,SystemClock.elapsedRealtime())
                    ColorAdjust.apply(fallbackRgb,brightness,saturation,whiteRed,whiteGreen,whiteBlue)
                    val elapsed = if(lastFrame == 0L) 40f else (now-lastFrame)/1_000_000f; lastFrame=now
                    val processed = smoother.apply(fallbackRgb,elapsed)
                    LiveStatus.analyzed++; LiveStatus.preview=processed.copyOf()
                    if(windowStart==0L) { windowStart=now; analyzedAtWindow=LiveStatus.analyzed; sentAtWindow=LiveStatus.sent; callbacksAtWindow=callbacks }
                    if(now-windowStart>=1_000_000_000L) { val seconds=(now-windowStart)/1_000_000_000.0; LiveStatus.captureFps=(LiveStatus.analyzed-analyzedAtWindow)/seconds; LiveStatus.callbackFps=(callbacks-callbacksAtWindow)/seconds; LiveStatus.outputFps=(LiveStatus.sent-sentAtWindow)/seconds; windowStart=now; analyzedAtWindow=LiveStatus.analyzed; sentAtWindow=LiveStatus.sent; callbacksAtWindow=callbacks }
                    LiveStatus.processingMs=(SystemClock.elapsedRealtimeNanos()-started)/1_000_000.0
                    val replaced = pending.getAndSet(LedFrame(processed,now)) != null
                    if (replaced) LiveStatus.drop()
                    rate.observe(LiveStatus.processingMs, LiveStatus.sendMs + LiveStatus.encodeMs, replaced)
                    LiveStatus.cap = rate.fps
                    val newWidth=resolution.observe(rate.fps,SystemClock.elapsedRealtime())
                    if(newWidth != null && !resizePending) { resizePending=true; handler.post { try { reconfigure(newWidth) } finally { resizePending=false } } }
                    dispatch(activeHost)
                } catch (e: Exception) { LiveStatus.text = "Frame processing error: ${e.javaClass.simpleName}" } finally { image.close() }
            },handler)
    }
    private fun dispatch(host: String) {
        if (closed || SystemClock.elapsedRealtime() < retryAt || !sending.compareAndSet(false,true)) return
        try { network.execute {
            try {
                if (sender == null) sender = DdpSender(host)
                while (true) {
                    val frame=pending.getAndSet(null) ?: break
                    val age=SystemClock.elapsedRealtimeNanos()-frame.sampledAtNanos
                    if(age>250_000_000L) { LiveStatus.drop(); continue }
                    LiveStatus.frameAgeMs=age/1_000_000.0
                    val timing=sender!!.send(frame.rgb); retryDelay=1000L
                    LiveStatus.encodeMs=timing.encodeNanos/1_000_000.0; LiveStatus.sendMs=timing.sendNanos/1_000_000.0; LiveStatus.sent++
                }
            } catch (e: Exception) { LiveStatus.text = "UDP error: ${e.javaClass.simpleName}"; sender?.close(); sender=null; retryAt=SystemClock.elapsedRealtime()+retryDelay; retryDelay=(retryDelay*2).coerceAtMost(30000L) }
            finally { sending.set(false); if (pending.get() != null) dispatch(host) }
        } } catch (_: java.util.concurrent.RejectedExecutionException) { sending.set(false) }
    }
    override fun onDestroy() {
        closed=true; if(sleepRegistered) { try { unregisterReceiver(sleepReceiver) } catch (_: Exception) { }; sleepRegistered=false }; prefs?.unregisterOnSharedPreferenceChangeListener(preferenceListener); networkCallback?.let { try { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } catch (_: Exception) { } }; LiveStatus.active=false; LiveStatus.preview=null; LiveStatus.captureFps=0.0; LiveStatus.callbackFps=0.0; LiveStatus.outputFps=0.0
        // ImageReader buffers remain valid until the capture thread finishes its current callback.
        // Closing the reader on the main thread while sampling can invalidate native memory.
        val captureWorker=worker
        val handler=captureHandler
        val cleanup = {
            reader?.setOnImageAvailableListener(null,null); display?.release(); reader?.close()
            projection?.unregisterCallback(callback); projection?.stop()
            reader=null; display=null; projection=null
            captureWorker?.quitSafely()
            Unit
        }
        if(handler == null || !handler.post(cleanup)) cleanup()
        pending.set(null); control.shutdownNow(); network.shutdownNow(); sender?.close(); LiveStatus.controller="Not checked"
        if (LiveStatus.text.startsWith("Capture active")) LiveStatus.text="Capture stopped"
        super.onDestroy()
    }
}
