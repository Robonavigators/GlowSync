package com.robonavigators.glowsync

import android.app.Activity
import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.media.projection.MediaProjectionConfig
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.robonavigators.glowsync.diagnostics.DiagnosticPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.robonavigators.glowsync.capture.*
import com.robonavigators.glowsync.network.DdpSender
import com.robonavigators.glowsync.network.WledDiscovery
import com.robonavigators.glowsync.network.WledDevice
import com.robonavigators.glowsync.data.ControllerStore
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread

@Composable
private fun GlowButton(onClick: () -> Unit, enabled: Boolean = true, autoFocus: Boolean = false, content: @Composable RowScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val requester=remember { FocusRequester() }
    LaunchedEffect(autoFocus,enabled) { if(autoFocus && enabled) requester.requestFocus() }
    androidx.compose.material3.Button(onClick=onClick,enabled=enabled,modifier=Modifier.focusRequester(requester).onFocusChanged { focused=it.isFocused }.border(if(focused) 3.dp else 1.dp,if(focused) Color(0xff74d5ff) else Color.White.copy(alpha=.18f),RoundedCornerShape(28.dp)),content=content)
}
@Composable
private fun GlowOutlinedButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    androidx.compose.material3.OutlinedButton(onClick=onClick,enabled=enabled,modifier=Modifier.onFocusChanged { focused=it.isFocused }.border(if(focused) 3.dp else 1.dp,if(focused) Color(0xff74d5ff) else Color.White.copy(alpha=.18f),RoundedCornerShape(28.dp)),content=content)
}

@Composable
private fun ResponseDiagnostic(host:String, count:Int, close:()->Unit) {
    var lit by remember { mutableStateOf(false) }
    var sender by remember { mutableStateOf<DdpSender?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Preparing UDP destination…") }
    val scope=rememberCoroutineScope()
    val disposed=remember { AtomicBoolean(false) }
    LaunchedEffect(host) {
        try {
            withContext(Dispatchers.IO) {
                val candidate=DdpSender(host)
                var assigned=false
                try {
                    withContext(Dispatchers.Main) {
                        if(!disposed.get()) { sender=candidate; assigned=true; result="Ready. Film the screen and LEDs together, then toggle the pattern." }
                    }
                } finally { if(!assigned) candidate.close() }
            }
        } catch(e:Exception) { if(!disposed.get()) result="Destination unavailable: ${e.javaClass.simpleName}" }
    }
    DisposableEffect(Unit) { onDispose { disposed.set(true); sender?.close() } }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(if(lit) Color.White else Color.Black)) {
            Column(Modifier.align(Alignment.BottomCenter).padding(24.dp).background(Color(0xff101b32).copy(alpha=.95f),RoundedCornerShape(18.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Physical response diagnostic",color=Color.White,fontSize=22.sp)
                Text("This direct UDP pattern bypasses capture. Film the screen and LEDs together with a high-speed camera; compare transitions frame by frame. For sync latency, film a test pattern in another app while GlowSync is active.",color=Color.White)
                Text(result,color=Color(0xff74d5ff))
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    GlowButton(enabled=sender!=null && !busy,onClick={
                        val target=!lit; lit=target; busy=true
                        scope.launch {
                            try {
                                val timing=withContext(Dispatchers.IO) { sender!!.send(DiagnosticPattern.frame(count,target)) }
                                result="UDP send call ${"%.2f".format(timing.sendNanos/1_000_000.0)} ms; delivery and physical output are unconfirmed."
                            } catch(e:Exception) { result="Send failed: ${e.javaClass.simpleName}" }
                            finally { busy=false }
                        }
                    }) { Text(if(lit) "Switch to black" else "Switch to white") }
                    GlowOutlinedButton(onClick=close) { Text("Close diagnostic") }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private fun captureIntent(): Intent {
        val manager=getSystemService(MediaProjectionManager::class.java)
        return if(Build.VERSION.SDK_INT>=34) manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()) else manager.createScreenCaptureIntent()
    }
    private val permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val host = getSharedPreferences("glowsync",MODE_PRIVATE).getString("host", "").orEmpty()
            try {
                startForegroundService(Intent(this,ProjectionService::class.java).putExtra("result",result.resultCode).putExtra("consent",result.data).putExtra("host",host))
            } catch (e: RuntimeException) { LiveStatus.text="Could not start capture service: ${e.javaClass.simpleName}" }
        } else { LiveStatus.text = "Screen capture denied" }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
        var ready by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
        LaunchedEffect(Unit) { if(!ready) { kotlinx.coroutines.delay(350); ready=true } }
        var complete by remember { mutableStateOf(getSharedPreferences("glowsync",MODE_PRIVATE).getBoolean("setup",false)) }
        if(!ready) Splash() else if (complete) Screen { complete=false } else SetupWizard { complete=true }
    } }
    @Composable private fun Splash() {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible=true }
        val alpha by animateFloatAsState(if(visible) 1f else 0f,label="logo reveal")
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xff284a79),Color(0xff101b32),Color(0xff080f1e)))),contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("◈",fontSize=48.sp,color=Color(0xff74d5ff).copy(alpha=alpha))
                Text("GlowSync",fontSize=38.sp,fontWeight=FontWeight.SemiBold,color=Color(0xffeaf3ff).copy(alpha=alpha))
                Text("By Robonavigators",fontSize=14.sp,color=Color(0xff74d5ff).copy(alpha=alpha))
            }
        }
    }
    @Composable fun SetupWizard(done: () -> Unit) {
        val prefs = remember { getSharedPreferences("glowsync",MODE_PRIVATE) }
        var step by remember { mutableIntStateOf(prefs.getInt("setupStep",0).coerceIn(0,5)) }
        var host by remember { mutableStateOf(prefs.getString("host", "").orEmpty()) }
        var message by remember { mutableStateOf("") }
        var verified by remember { mutableStateOf(false) }
        val setupStore=remember { ControllerStore(prefs) }
        var setupFound by remember { mutableStateOf<List<WledDevice>>(emptyList()) }
        var setupDiscovery by remember { mutableStateOf<WledDiscovery?>(null) }
        val activeDiscovery by rememberUpdatedState(setupDiscovery)
        DisposableEffect(Unit) { onDispose { activeDiscovery?.close() } }
        var counts by remember { mutableStateOf(listOf("top","right","bottom","left").map { prefs.getInt(it, if(it=="top" || it=="bottom") 30 else 18).toString() }) }
        var setupBrightness by remember { mutableFloatStateOf(prefs.getFloat("brightness",1f)) }
        var setupSaturation by remember { mutableFloatStateOf(prefs.getFloat("saturation",1f)) }
        var setupDepth by remember { mutableFloatStateOf(prefs.getFloat("depth",.1f)) }
        var setupSmoothing by remember { mutableIntStateOf(prefs.getInt("smoothing",80)) }
        var setupFps by remember { mutableIntStateOf(prefs.getInt("fps",25)) }
        var setupBars by remember { mutableStateOf(prefs.getBoolean("bars",false)) }
        val setupWhite=remember { mutableStateListOf<Float>().apply { listOf("whiteRed","whiteGreen","whiteBlue").forEach { add(prefs.getFloat(it,1f)) } } }
        var setupOffset by remember { mutableStateOf(prefs.getInt("offset",0).toString()) }
        var setupReverse by remember { mutableStateOf(prefs.getBoolean("reverse",false)) }
        var setupMarker by remember { mutableIntStateOf(0) }
        var previewTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { while(true) { kotlinx.coroutines.delay(1000); previewTick++ } }
        fun go(next: Int) { step=next; prefs.edit().putInt("setupStep",next).apply() }
        BackHandler(enabled=step>0) { go(step-1) }
        MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xff74d5ff), surface=Color(0xff17243b))) {
            CompositionLocalProvider(LocalContentColor provides Color(0xffeaf3ff)) {
            Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xff080f1e),Color(0xff182c50),Color(0xff080f1e)))).verticalScroll(rememberScrollState()).padding(40.dp).background(Color(0xff101b32).copy(alpha=.62f),RoundedCornerShape(24.dp)).border(1.dp,Color.White.copy(alpha=.12f),RoundedCornerShape(24.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("GLOWSYNC  /  BY ROBONAVIGATORS",color=Color(0xff74d5ff),letterSpacing=2.sp)
                Text("SETUP  ${step+1} / 6",color=Color.LightGray)
                LinearProgressIndicator(progress={ (step+1)/6f },modifier=Modifier.width(440.dp))
                when(step) {
                    0 -> {
                        Text("Your Screen. Beyond Its Borders.",fontSize=30.sp)
                        Text("Bring every supported scene to life with immersive ambient lighting.")
                        Canvas(Modifier.width(360.dp).height(140.dp)) {
                            val inset=18f; drawRoundRect(Color(0xff0a1223),Offset(inset,inset),Size(size.width-inset*2,size.height-inset*2),androidx.compose.ui.geometry.CornerRadius(10f))
                            for(i in 0..20) { val x=inset+i*(size.width-inset*2)/20; drawCircle(if(i%2==0) Color(0xff4da8ff) else Color(0xff9369f8),4f,Offset(x,inset)); drawCircle(Color(0xff74d5ff),3f,Offset(x,size.height-inset)) }
                        }
                        Text("Illustration",color=Color.LightGray,fontSize=12.sp)
                        GlowButton(onClick={go(1)},autoFocus=true) { Text("Get Started") }
                    }
                    1 -> {
                        Text("Connect Your Lights",fontSize=30.sp)
                        Text("Discover a controller or enter its address. Results are verified through WLED before they are listed.")
                        GlowOutlinedButton(onClick={ setupDiscovery?.close(); setupFound=emptyList(); message="Scanning local network…"; setupDiscovery=WledDiscovery(this@MainActivity,{ device -> runOnUiThread { if(setupFound.none { it.host==device.host }) setupFound=setupFound+device } }, { error -> runOnUiThread { message=error } }).also { it.start() } }) { Text("Discover WLED") }
                        setupFound.forEach { device -> GlowOutlinedButton(onClick={ host=device.host; verified=true; message="Verified ${device.name}: ${device.ledCount} LEDs, firmware ${device.version}"; prefs.edit().putString("host",host).apply(); setupStore.save(device); setupDiscovery?.close(); setupDiscovery=null }) { Text("${device.name} · ${device.host} · ${device.ledCount} LEDs") } }
                        OutlinedTextField(value=host,onValueChange={host=it; verified=false},label={Text("IP address or hostname")},singleLine=true)
                        GlowButton(onClick={ val h=host.trim(); thread { try { val d=WledDiscovery.verify(h); runOnUiThread { verified=true; message="Verified ${d.name}: ${d.ledCount} LEDs, firmware ${d.version}"; prefs.edit().putString("host",h).apply(); setupStore.save(d) } } catch(e:Exception) { runOnUiThread { verified=false; message="Unable to verify: ${e.javaClass.simpleName}" } } } }) { Text("Test connection") }
                        Text(message); GlowButton(enabled=verified,onClick={setupDiscovery?.close(); setupDiscovery=null; go(2)}) { Text("Continue") }
                    }
                    2 -> {
                        Text("Map Your Ambilight",fontSize=30.sp); Text("Enter LED counts clockwise from the top-left. Use zero for an absent side. Offset and reversal can be adjusted on the dashboard.")
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Top","Right","Bottom","Left").forEachIndexed { i,label -> OutlinedTextField(value=counts[i],onValueChange={ value -> counts=counts.toMutableList().also { it[i]=value.filter(Char::isDigit).take(4) } },label={Text(label)},singleLine=true,modifier=Modifier.width(110.dp)) } }
                        Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checked=setupReverse,onCheckedChange={setupReverse=it}); Text("Reverse direction") }
                        OutlinedTextField(value=setupOffset,onValueChange={setupOffset=it.filter(Char::isDigit).take(4)},label={Text("LED offset")},singleLine=true,modifier=Modifier.width(160.dp))
                        GlowOutlinedButton(enabled=!LiveStatus.active,onClick={ val count=counts.sumOf { it.toIntOrNull() ?: 0 }.coerceIn(1,2000); val selected=setupMarker%count; thread { try { DdpSender(host.trim()).use { sender -> val frame=ByteArray(count*3); frame[selected*3]=100; sender.send(frame) }; runOnUiThread { message="Sent marker at physical LED $selected; verify its position visually."; setupMarker=(selected+1)%count } } catch(e:Exception) { runOnUiThread { message="Marker failed: ${e.javaClass.simpleName}" } } } }) { Text("Test next LED ($setupMarker)") }
                        GlowButton(onClick={ val n=counts.map { it.toIntOrNull() ?: 0 }; if(n.all { it in 0..1000 } && n.sum() in 1..2000) { prefs.edit().putInt("top",n[0]).putInt("right",n[1]).putInt("bottom",n[2]).putInt("left",n[3]).putInt("offset",setupOffset.toIntOrNull() ?: 0).putBoolean("reverse",setupReverse).apply(); go(3) } else message="Enter 1–2000 LEDs total, up to 1000 per side." }) { Text("Save and continue") }; Text(message)
                    }
                    3 -> {
                        Text("Bring Your Screen to Life",fontSize=30.sp); Text("GlowSync analyzes permitted screen colours locally. Android will ask for your consent; protected content may be unavailable.")
                        GlowButton(onClick={ permission.launch(captureIntent()) },autoFocus=true) { Text("Allow Screen Capture") }
                        Text(if(LiveStatus.active.also { previewTick }) "Capture is active for this session." else "Capture inactive: ${LiveStatus.text}")
                        GlowButton(onClick={go(4)}) { Text(if(LiveStatus.active.also { previewTick }) "Continue" else "Continue without capture") }
                    }
                    4 -> {
                        Text("Make Every Colour Perfect",fontSize=30.sp)
                        Text("Adjust the active session. Open supported content to judge the lights; protected content can appear black.")
                        Text("Brightness ${"%.0f".format(setupBrightness*100)}%")
                        Slider(value=setupBrightness,onValueChange={setupBrightness=it; prefs.edit().putFloat("brightness",it).apply()},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                        Text("Saturation ${"%.0f".format(setupSaturation*100)}%")
                        Slider(value=setupSaturation,onValueChange={setupSaturation=it; prefs.edit().putFloat("saturation",it).apply()},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                        Text("Edge depth ${"%.0f".format(setupDepth*100)}%")
                        Slider(value=setupDepth,onValueChange={setupDepth=it; prefs.edit().putFloat("depth",it).apply()},valueRange=.01f..0.5f,modifier=Modifier.width(400.dp))
                        Text("White balance")
                        listOf("Red","Green","Blue").forEachIndexed { index,label ->
                            val key=listOf("whiteRed","whiteGreen","whiteBlue")[index]
                            Text("$label ${"%.0f".format(setupWhite[index]*100)}%")
                            Slider(value=setupWhite[index],onValueChange={setupWhite[index]=it; prefs.edit().putFloat(key,it).apply()},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                        }
                        Text("Smoothing ${setupSmoothing} ms")
                        Slider(value=setupSmoothing.toFloat(),onValueChange={setupSmoothing=it.toInt(); prefs.edit().putInt("smoothing",setupSmoothing).apply()},valueRange=0f..1000f,modifier=Modifier.width(400.dp))
                        Text("Target $setupFps FPS; 60 FPS may not be available on all devices")
                        Slider(value=setupFps.toFloat(),onValueChange={setupFps=it.toInt().coerceIn(10,60); prefs.edit().putInt("fps",setupFps).apply()},valueRange=10f..60f,steps=49,modifier=Modifier.width(400.dp))
                        Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checked=setupBars,onCheckedChange={setupBars=it; prefs.edit().putBoolean("bars",it).apply()}); Text("Conservative black-bar detection") }
                        val colors=LiveStatus.preview.also { previewTick }
                        Text(if(LiveStatus.active && colors!=null) "Live computed LED colours" else "Preview unavailable — capture inactive")
                        Canvas(Modifier.width(400.dp).height(16.dp)) { if(colors!=null && colors.isNotEmpty()) { val n=colors.size/3; repeat(n) { i -> drawRect(Color(colors[i*3].toInt() and 255,colors[i*3+1].toInt() and 255,colors[i*3+2].toInt() and 255),Offset(i*size.width/n,0f),Size(size.width/n+1,size.height)) } } }
                        GlowButton(onClick={go(5)}) { Text("Continue") }
                    }
                    else -> { Text("Your GlowSync Is Ready",fontSize=30.sp); Text("Controller: ${prefs.getString("host", "Not configured")}\nLEDs: ${listOf("top","right","bottom","left").sumOf { prefs.getInt(it,0) }}\nCapture: ${if(LiveStatus.active.also { previewTick }) "active" else "inactive"}\nPerformance: ${prefs.getInt("fps",25)} FPS target")
                        GlowButton(onClick={ prefs.edit().putBoolean("setup",true).apply(); done() }) { Text("Enter GlowSync") } }
                }
                if(step>0) GlowOutlinedButton(onClick={go(step-1)}) { Text("Back") }
            }
            }
        }
    }
    @OptIn(ExperimentalLayoutApi::class)
    @Composable fun Screen(reset: () -> Unit) {
        val prefs = remember { getSharedPreferences("glowsync",MODE_PRIVATE) }
        var host by remember { mutableStateOf(prefs.getString("host", "").orEmpty()) }
        var top by remember { mutableStateOf(prefs.getInt("top",30).toString()) }; var right by remember { mutableStateOf(prefs.getInt("right",18).toString()) }
        var bottom by remember { mutableStateOf(prefs.getInt("bottom",30).toString()) }; var left by remember { mutableStateOf(prefs.getInt("left",18).toString()) }
        var offset by remember { mutableStateOf(prefs.getInt("offset",0).toString()) }; var reverse by remember { mutableStateOf(prefs.getBoolean("reverse",false)) }
        val controllerStore=remember { ControllerStore(prefs) }
        var saved by remember { mutableStateOf(controllerStore.all()) }
        var found by remember { mutableStateOf<List<WledDevice>>(emptyList()) }
        var discovery by remember { mutableStateOf<WledDiscovery?>(null) }
        val currentDiscovery by rememberUpdatedState(discovery)
        DisposableEffect(Unit) { onDispose { currentDiscovery?.close() } }
        var brightness by remember { mutableFloatStateOf(prefs.getFloat("brightness",1f)) }
        var saturation by remember { mutableFloatStateOf(prefs.getFloat("saturation",1f)) }
        var depth by remember { mutableFloatStateOf(prefs.getFloat("depth",.1f)) }
        val sideGain=remember { mutableStateListOf<Float>().apply { repeat(12) { add(prefs.getFloat("sideGain$it",1f)) } } }
        var weighted by remember { mutableStateOf(prefs.getBoolean("weighted",false)) }
        var whiteRed by remember { mutableFloatStateOf(prefs.getFloat("whiteRed",1f)) }
        var whiteGreen by remember { mutableFloatStateOf(prefs.getFloat("whiteGreen",1f)) }
        var whiteBlue by remember { mutableFloatStateOf(prefs.getFloat("whiteBlue",1f)) }
        var bars by remember { mutableStateOf(prefs.getBoolean("bars",false)) }
        var smoothing by remember { mutableIntStateOf(prefs.getInt("smoothing",80)) }
        var fallback by remember { mutableStateOf(prefs.getString("fallback","PASS_THROUGH") ?: "PASS_THROUGH") }
        var marker by remember { mutableIntStateOf(0) }
        var fps by remember { mutableIntStateOf(prefs.getInt("fps",25)) }
        var showDiagnostic by remember { mutableStateOf(false) }
        var externalLatency by remember { mutableStateOf(prefs.getString("externalLatency", "").orEmpty()) }
        var info by remember { mutableStateOf("Enter a WLED IP address or hostname, then test the connection.") }
        var page by rememberSaveable { mutableIntStateOf(0) }
        val scroll=rememberScrollState()
        LaunchedEffect(page) { scroll.scrollTo(0) }
        BackHandler(enabled=page!=0) { page=0 }
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); tick++ } }
        val navy=Color(0xff080f1e); val white=Color(0xffeaf3ff); val blue=Color(0xff74d5ff)
        MaterialTheme(colorScheme=darkColorScheme(primary=blue, surface=Color(0xff17243b), onSurface=white)) {
            CompositionLocalProvider(LocalContentColor provides white) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(navy,Color(0xff16264a),navy)))) {
                Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal=40.dp, vertical=24.dp).background(Color(0xff101b32).copy(alpha=.62f),RoundedCornerShape(24.dp)).border(1.dp,white.copy(alpha=.12f),RoundedCornerShape(24.dp)).padding(24.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text("GlowSync", color=white, fontSize=34.sp, fontWeight=FontWeight.SemiBold)
                    Text("BY ROBONAVIGATORS  /  AMBIENT LIGHTING",color=blue,fontSize=12.sp,letterSpacing=2.sp)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("Home","Devices","Calibration","Settings").forEachIndexed { index,label ->
                            GlowOutlinedButton(onClick={page=index}) { Text(if(page==index) "● $label" else label) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if(page==0) {
                    Text("Your screen. Beyond its borders.",color=white,fontSize=25.sp)
                    val preview=LiveStatus.preview.also { tick }
                    Text(if (LiveStatus.active && preview != null) "Live computed LED colours" else "Preview — sync inactive",color=blue,fontSize=12.sp)
                    Canvas(Modifier.width(420.dp).height(200.dp)) {
                        val values=preview
                        val sideCounts=listOf(prefs.getInt("top",30),prefs.getInt("right",18),prefs.getInt("bottom",30),prefs.getInt("left",18))
                        val total=sideCounts.sum().coerceAtLeast(1); val shift=prefs.getInt("offset",0); val reversed=prefs.getBoolean("reverse",false)
                        val x0=16f; val y0=16f; val w=size.width-32f; val h=size.height-32f
                        val haloColor=if(values!=null && values.size>=3) Color(values[0].toInt() and 255,values[1].toInt() and 255,values[2].toInt() and 255) else Color(0xff4da8ff)
                        val center=Offset(size.width/2f,size.height/2f)
                        drawCircle(Brush.radialGradient(listOf(haloColor.copy(alpha=.24f),Color.Transparent),center=center,radius=size.width*.55f),radius=size.width*.55f,center=center)
                        drawRoundRect(Brush.linearGradient(listOf(Color(0xff1d3153),Color(0xff0a1222))),Offset(x0,y0),Size(w,h),androidx.compose.ui.geometry.CornerRadius(8f,8f))
                        var canonical=0
                        sideCounts.forEachIndexed { side,count -> repeat(count) { i ->
                            val physical=Math.floorMod((if(reversed) (total-canonical)%total else canonical)+shift,total)
                            val c=if(values != null && values.size >= physical*3+3) Color(values[physical*3].toInt() and 255,values[physical*3+1].toInt() and 255,values[physical*3+2].toInt() and 255) else if(canonical%2==0) Color(0xff4da8ff) else Color(0xff9369f8)
                            val t=(i+.5f)/count
                            val point=when(side) { 0 -> Offset(x0+t*w,y0); 1 -> Offset(x0+w,y0+t*h); 2 -> Offset(x0+(1-t)*w,y0+h); else -> Offset(x0,y0+(1-t)*h) }
                            drawCircle(c,4f,point); canonical++
                        } }
                    }
                    Text("GlowSync analyzes permitted screen colours locally. Protected video and HDMI inputs may be unavailable.",color=white.copy(alpha=.76f),fontSize=15.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        GlowButton(autoFocus=true,enabled=!LiveStatus.active,onClick={ if(host.isBlank()) info="Enter a controller address first." else { prefs.edit().putString("host",host.trim()).apply(); permission.launch(captureIntent()) } }) { Text("Allow Screen Capture / Start Sync") }
                        GlowOutlinedButton(onClick={ stopService(Intent(this@MainActivity,ProjectionService::class.java)); LiveStatus.text="Capture stopped" }) { Text("Stop Sync") }
                    }
                    if(host.isBlank()) Text("Enter a controller in Devices before starting sync.",color=blue)
                    Text("${LiveStatus.text.also { tick }}   •   Analyzed ${LiveStatus.analyzed}   •   Dropped ${LiveStatus.dropped}   •   UDP frames ${LiveStatus.sent}   •   Processing ${"%.1f".format(LiveStatus.processingMs)} ms   •   Callbacks ${"%.1f".format(LiveStatus.callbackFps)} fps   •   Analyzed ${"%.1f".format(LiveStatus.captureFps)} fps   •   UDP ${"%.1f".format(LiveStatus.outputFps)} fps   •   Cap ${LiveStatus.cap} fps",color=blue,fontSize=14.sp)
                    Text("Controller: ${LiveStatus.controller.also { tick }}",color=white.copy(alpha=.75f),fontSize=13.sp)
                    Text("Diagnostics: encode ${"%.2f".format(LiveStatus.encodeMs)} ms · UDP call ${"%.2f".format(LiveStatus.sendMs)} ms · frame age since acquisition at dispatch ${"%.1f".format(LiveStatus.frameAgeMs)} ms. UDP has no delivery acknowledgement.",color=white.copy(alpha=.65f),fontSize=13.sp)
                    }
                    if(page==1) {
                    Text("Connect your lights",fontSize=22.sp,color=white)
                    OutlinedTextField(value=host,onValueChange={host=it},label={Text("WLED IP address or hostname")},singleLine=true,modifier=Modifier.width(420.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        GlowButton(onClick={ discovery?.close(); found=emptyList(); info="Scanning local network; devices are verified before listing."; discovery=WledDiscovery(this@MainActivity, { device -> runOnUiThread { if(found.none { it.host == device.host }) found=found+device } }, { error -> runOnUiThread { info=error } }).also { it.start() } }) { Text("Discover WLED") }
                        GlowButton(onClick={ val h=host.trim(); thread { val message=try { val d=WledDiscovery.verify(h); controllerStore.save(d); runOnUiThread { saved=controllerStore.all() }; "Verified: ${d.name} · ${d.version} · ${d.ledCount} LEDs" } catch(e:Exception) { "Connection failed: ${e.javaClass.simpleName}" }; runOnUiThread { info=message } } }) { Text("Test connection") }
                        GlowOutlinedButton(enabled=!LiveStatus.active,onClick={ val h=host.trim(); val count=listOf(top,right,bottom,left).sumOf { it.toIntOrNull() ?: 0 }.coerceIn(1,2000); thread { try { DdpSender(h).use { sender -> val rgb=ByteArray(count*3); for(i in 0 until count) rgb[i*3+ i%3]=100; sender.send(rgb) }; runOnUiThread { info="Test frame sent via UDP; check the LEDs." } } catch(e:Exception) { runOnUiThread { info="Test failed: ${e.javaClass.simpleName}" } } } }) { Text("Send LED test") }
                    }
                    GlowOutlinedButton(enabled=!LiveStatus.active,onClick={ val h=host.trim(); val count=listOf(top,right,bottom,left).sumOf { it.toIntOrNull() ?: 0 }.coerceIn(1,2000); val selected=marker%count; thread { try { DdpSender(h).use { sender -> val rgb=ByteArray(count*3); rgb[selected*3]=100; sender.send(rgb) }; runOnUiThread { info="Sent marker at physical index $selected; UDP delivery is unconfirmed."; marker=(selected+1)%count } } catch(e:Exception) { runOnUiThread { info="Marker failed: ${e.javaClass.simpleName}" } } } }) { Text("Send next LED marker ($marker)") }
                    if(LiveStatus.active) Text("Stop sync before sending a manual LED test.",color=white.copy(alpha=.7f))
                    Text(info,color=white.copy(alpha=.8f),fontSize=14.sp)
                    if(saved.isNotEmpty()) Text("Saved controllers (last verified information)",color=white)
                    saved.forEach { device -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        GlowOutlinedButton(onClick={ host=device.host; info="Selected ${device.name}. Start a new capture session to change destination." }) { Text("${device.name} · ${device.host}") }
                        GlowOutlinedButton(onClick={ controllerStore.remove(device.host); saved=controllerStore.all() }) { Text("Remove") }
                    } }
                    found.forEach { device -> GlowOutlinedButton(onClick={ host=device.host; controllerStore.save(device); saved=controllerStore.all(); info="Selected ${device.name}, ${device.ledCount} LEDs, firmware ${device.version}. Save configuration to retain it." }) { Text("${device.name} · ${device.host} · ${device.ledCount} LEDs") } }
                    }
                    if(page==2) {
                    Text("Map your Ambilight",fontSize=22.sp,color=white)
                    Text("Clockwise from top-left. Set bottom to 0 for a three-sided strip; use offset and reverse to match physical wiring.",color=white.copy(alpha=.75f))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        @Composable fun number(label:String,value:String,change:(String)->Unit) { OutlinedTextField(value=value,onValueChange={ change(it.filter(Char::isDigit).take(4)) },label={Text(label)},singleLine=true,modifier=Modifier.width(110.dp)) }
                        number("Top",top){top=it}; number("Right",right){right=it}; number("Bottom",bottom){bottom=it}; number("Left",left){left=it}; number("Offset",offset){offset=it}
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checked=reverse,onCheckedChange={reverse=it}); Text("Reverse strip direction") }
                    Text("Starting corner (sets physical LED 0)")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("Top left","Top right","Bottom right","Bottom left").forEachIndexed { corner,label ->
                            GlowOutlinedButton(onClick={ val counts=listOf(top,right,bottom,left).map { it.toIntOrNull() ?: 0 }; val total=counts.sum(); if(total>0) { val canonical=when(corner) { 0 -> 0; 1 -> counts[0]; 2 -> counts[0]+counts[1]; else -> counts[0]+counts[1]+counts[2] } % total; val path=if(reverse) (total-canonical)%total else canonical; offset=Math.floorMod(-path,total).toString() } }) { Text(label) }
                        }
                    }
                    Text("Colour and response",fontSize=22.sp,color=white)
                    Text("Brightness ${"%.0f".format(brightness*100)}%")
                    Slider(value=brightness,onValueChange={brightness=it},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                    Text("Saturation ${"%.0f".format(saturation*100)}%")
                    Slider(value=saturation,onValueChange={saturation=it},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { GlowOutlinedButton(onClick={saturation=1f; weighted=false}) { Text("Natural") }; GlowOutlinedButton(onClick={saturation=1.4f; weighted=true}) { Text("Vibrant") } }
                    Text("Edge depth ${"%.0f".format(depth*100)}%")
                    Slider(value=depth,onValueChange={depth=it},valueRange=.01f..0.5f,modifier=Modifier.width(400.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checked=weighted,onCheckedChange={weighted=it}); Text("Weight pixels nearer the screen edge") }
                    Text("White balance — red ${"%.0f".format(whiteRed*100)}%")
                    Slider(value=whiteRed,onValueChange={whiteRed=it},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                    Text("White balance — green ${"%.0f".format(whiteGreen*100)}%")
                    Slider(value=whiteGreen,onValueChange={whiteGreen=it},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                    Text("White balance — blue ${"%.0f".format(whiteBlue*100)}%")
                    Slider(value=whiteBlue,onValueChange={whiteBlue=it},valueRange=0f..2f,modifier=Modifier.width(400.dp))
                    Text("Per-side RGB correction (100% is neutral)")
                    listOf("Top","Right","Bottom","Left").forEachIndexed { side,label ->
                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(label,modifier=Modifier.width(65.dp))
                            listOf("R","G","B").forEachIndexed { channel,channelLabel ->
                                val index=side*3+channel
                                Column(Modifier.width(120.dp)) { Text("$channelLabel ${"%.0f".format(sideGain[index]*100)}%",fontSize=12.sp); Slider(value=sideGain[index],onValueChange={ sideGain[index]=it },valueRange=0f..2f) }
                            }
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checked=bars,onCheckedChange={bars=it}); Text("Conservative black-bar detection") }
                    Text("When the captured image is completely dark")
                    Text("Dark scenes and blocked content can look identical. Choose how the lights respond.",color=white.copy(alpha=.75f))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Follow screen" to "PASS_THROUGH", "Hold 2 seconds" to "HOLD_THEN_DARK", "Static warm" to "STATIC").forEach { (label,value) -> GlowOutlinedButton(onClick={ fallback=value }) { Text(if(fallback==value) "✓ $label" else label) } } }
                    Text("Smoothing: ${smoothing} ms")
                    Slider(value=smoothing.toFloat(),onValueChange={smoothing=it.toInt()},valueRange=0f..1000f,modifier=Modifier.width(400.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Ultra Fast" to 0, "Balanced" to 80, "Cinematic" to 250).forEach { (label,value) -> GlowOutlinedButton(onClick={ smoothing=value }) { Text(label) } } }
                    }
                    if(page==3) {
                    Text("Performance target: $fps FPS (adaptive; up to 60 may not be sustainable)",color=white)
                    Slider(value=fps.toFloat(),onValueChange={fps=it.toInt().coerceIn(10,60)},valueRange=10f..60f,steps=49,modifier=Modifier.width(400.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Eco" to 15, "Balanced" to 25, "Fast" to 30, "Ultra" to 60).forEach { (label,value) -> GlowOutlinedButton(onClick={ fps=value }) { Text(label) } } }
                    }
                    if(page==2 || page==3) {
                    GlowButton(onClick={ val counts=listOf(top,right,bottom,left).map { it.toIntOrNull() ?: 0 }; if(counts.all { it in 0..1000 } && counts.sum() in 1..2000) { prefs.edit().also { editor -> sideGain.forEachIndexed { index,value -> editor.putFloat("sideGain$index",value) } }.putString("host",host.trim()).putInt("top",counts[0]).putInt("right",counts[1]).putInt("bottom",counts[2]).putInt("left",counts[3]).putInt("offset",offset.toIntOrNull() ?: 0).putBoolean("reverse",reverse).putInt("fps",fps).putFloat("brightness",brightness).putFloat("saturation",saturation).putFloat("depth",depth).putBoolean("bars",bars).putBoolean("weighted",weighted).putFloat("whiteRed",whiteRed).putFloat("whiteGreen",whiteGreen).putFloat("whiteBlue",whiteBlue).putInt("smoothing",smoothing).putString("fallback",fallback).apply(); info="Settings saved and applied to capture. Restart sync if the controller address changed." } else info="Use 1–2000 LEDs total, with no more than 1000 per side." }) { Text("Save configuration") }
                    Text(info,color=white.copy(alpha=.8f),fontSize=14.sp)
                    }
                    if(page==3) {
                    Text("Physical response diagnostic",fontSize=22.sp,color=white)
                    Text("This direct UDP test bypasses screen capture. Film both transitions together or use photodiodes; the app does not measure visible LED response.",color=white.copy(alpha=.8f))
                    GlowOutlinedButton(enabled=!LiveStatus.active && host.isNotBlank(),onClick={showDiagnostic=true}) { Text("Open response test") }
                    if(LiveStatus.active) Text("Stop sync before running the response test.",color=white.copy(alpha=.7f))
                    OutlinedTextField(value=externalLatency,onValueChange={ externalLatency=it.filter { c -> c.isDigit() || c=='.' }.take(8) },label={Text("Externally measured latency (ms)")},singleLine=true,modifier=Modifier.width(320.dp))
                    GlowOutlinedButton(onClick={ val value=externalLatency.toDoubleOrNull(); if(externalLatency.isBlank() || (value != null && value in 0.0..10000.0)) { prefs.edit().putString("externalLatency",externalLatency).apply(); info=if(externalLatency.isBlank()) "External measurement cleared." else "External measurement saved: $externalLatency ms." } else info="Enter a valid measurement from 0 to 10000 ms." }) { Text("Save external measurement") }
                    Text(info,color=white.copy(alpha=.8f),fontSize=14.sp)
                    if(showDiagnostic) ResponseDiagnostic(host.trim(),listOf(top,right,bottom,left).sumOf { it.toIntOrNull() ?: 0 }.coerceIn(1,2000)) { showDiagnostic=false }
                    Text("Privacy: screen frames stay on this device and are not saved. DDP uses unencrypted UDP; use a trusted network.",color=white.copy(alpha=.65f),fontSize=13.sp)
                    var confirmClear by remember { mutableStateOf(false) }
                    GlowOutlinedButton(onClick={ confirmClear=true }) { Text("Clear local data and reset setup") }
                    if(confirmClear) AlertDialog(onDismissRequest={confirmClear=false},title={Text("Clear GlowSync data?")},text={Text("Saved controllers and calibration will be removed and active sync will stop.")},confirmButton={GlowButton(onClick={stopService(Intent(this@MainActivity,ProjectionService::class.java)); prefs.edit().clear().apply(); confirmClear=false; reset()}) { Text("Clear data") }},dismissButton={GlowOutlinedButton(onClick={confirmClear=false}) { Text("Cancel") }})
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            }
        }
    }
}
