package com.robonavigators.glowsync

import android.content.Context
import android.content.Intent
import com.robonavigators.glowsync.capture.LiveStatus
import com.robonavigators.glowsync.capture.ProjectionService
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.view.accessibility.AccessibilityNodeInfo
import android.app.UiAutomation
import com.robonavigators.glowsync.data.ControllerStore
import com.robonavigators.glowsync.network.WledDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifecycleTest {
    @Test fun remoteSelectAdvancesWelcome() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("glowsync",Context.MODE_PRIVATE); val previous=prefs.all.toMap()
        prefs.edit().putBoolean("setup",false).putInt("setupStep",0).commit()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
                fun hasWelcome(node:AccessibilityNodeInfo?):Boolean {
                    if(node==null) return false
                    if(node.text?.toString()=="Get Started") return true
                    for(i in 0 until node.childCount) if(hasWelcome(node.getChild(i))) return true
                    return false
                }
                var ready=false
                repeat(40) { if(!ready) { Thread.sleep(250); ready=hasWelcome(automation.rootInActiveWindow) } }
                assertTrue("Welcome button did not appear",ready)
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                Thread.sleep(300)
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
                var advanced=false
                repeat(30) { if(!advanced) { Thread.sleep(100); advanced=prefs.getInt("setupStep",0)==1 } }
                assertTrue("D-pad select did not advance setup",advanced)
            }
        } finally {
            val edit=prefs.edit().clear(); previous.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Int -> edit.putInt(key,value); is Boolean -> edit.putBoolean(key,value); is Float -> edit.putFloat(key,value); is Long -> edit.putLong(key,value); is Set<*> -> edit.putStringSet(key,value.filterIsInstance<String>().toSet()) } }; edit.commit()
        }
    }
    @Test fun activitySurvivesBackgroundAndRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }
    @Test fun userCanDenyRealCapturePrompt() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("glowsync",Context.MODE_PRIVATE)
        val previous=prefs.all.toMap()
        prefs.edit().putBoolean("setup",false).putInt("setupStep",3).putString("host","127.0.0.1").commit()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        fun find(node:AccessibilityNodeInfo?, text:String): AccessibilityNodeInfo? {
            if(node==null) return null
            if(node.text?.toString()==text) return node
            for(i in 0 until node.childCount) { val result=find(node.getChild(i),text); if(result!=null) return result }
            return null
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                var node:AccessibilityNodeInfo?=null
                repeat(40) { if(node==null) { Thread.sleep(250); node=find(automation.rootInActiveWindow,"Allow Screen Capture") } }
                assertNotNull("Capture button missing",node)
                var target=node
                while(target!=null && !target!!.isClickable) target=target!!.parent
                assertTrue("Could not open the capture request",target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
                var systemVisible=false
                repeat(40) { if(!systemVisible) { Thread.sleep(250); val pkg=automation.rootInActiveWindow?.packageName?.toString(); systemVisible=pkg!=null && pkg!=context.packageName } }
                assertTrue("Android capture consent prompt did not appear",systemVisible)
                automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                var denied=false
                repeat(40) { if(!denied) { Thread.sleep(250); denied=LiveStatus.text=="Screen capture denied" } }
                assertTrue("Denial was not handled",denied)
                assertFalse(LiveStatus.active)
            }
        } finally {
            val edit=prefs.edit().clear()
            previous.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Int -> edit.putInt(key,value); is Boolean -> edit.putBoolean(key,value); is Float -> edit.putFloat(key,value); is Long -> edit.putLong(key,value); is Set<*> -> edit.putStringSet(key,value.filterIsInstance<String>().toSet()) } }
            edit.commit()
        }
    }
    @Test fun acceptedProjectionSendsDdpToLoopbackAndStops() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("glowsync",Context.MODE_PRIVATE); val previous=prefs.all.toMap()
        prefs.edit().putBoolean("setup",false).putInt("setupStep",3).putString("host","127.0.0.1").commit()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        fun find(node:AccessibilityNodeInfo?, text:String): AccessibilityNodeInfo? {
            if(node==null) return null
            if(node.text?.toString()==text) return node
            for(i in 0 until node.childCount) { val result=find(node.getChild(i),text); if(result!=null) return result }
            return null
        }
        fun click(text:String) {
            var node:AccessibilityNodeInfo?=null
            repeat(40) { if(node==null) { Thread.sleep(250); node=find(automation.rootInActiveWindow,text) } }
            assertNotNull("Missing button $text",node)
            var target=node
            while(target!=null && !target!!.isClickable) target=target!!.parent
            assertTrue("Cannot click $text",target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
        }
        try {
            java.net.DatagramSocket(4048,java.net.InetAddress.getByName("127.0.0.1")).use { receiver ->
                receiver.soTimeout=10000
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    click("Allow Screen Capture"); click("Share screen")
                    val packet=java.net.DatagramPacket(ByteArray(1500),1500); receiver.receive(packet)
                    assertTrue(LiveStatus.active)
                    assertTrue(packet.length>10)
                    assertEquals(0x40,packet.data[0].toInt() and 0xfe)
                    assertEquals(0x0b,packet.data[2].toInt() and 255)
                    prefs.edit().putFloat("brightness",.8f).putFloat("depth",.15f).putInt("fps",15).apply()
                    Thread.sleep(500)
                    assertTrue("Capture stopped while applying calibration",LiveStatus.active)
                    scenario.onActivity { it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                    Thread.sleep(1000)
                    assertTrue("Projection stopped after orientation request",LiveStatus.active)
                    scenario.recreate()
                    assertTrue("Projection should survive activity recreation",LiveStatus.active)
                    scenario.moveToState(Lifecycle.State.CREATED)
                    assertTrue("Projection should survive activity backgrounding",LiveStatus.active)
                    val stop=android.app.PendingIntent.getService(context,1,Intent(context,ProjectionService::class.java).setAction("stop"),android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
                    stop.send()
                    var stopped=false; repeat(40) { if(!stopped) { Thread.sleep(100); stopped=!LiveStatus.active } }
                    assertTrue("Service did not stop",stopped)
                }
            }
        } finally {
            context.stopService(Intent(context,ProjectionService::class.java))
            val edit=prefs.edit().clear()
            previous.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Int -> edit.putInt(key,value); is Boolean -> edit.putBoolean(key,value); is Float -> edit.putFloat(key,value); is Long -> edit.putLong(key,value); is Set<*> -> edit.putStringSet(key,value.filterIsInstance<String>().toSet()) } }; edit.commit()
        }
    }
    @Test fun dashboardSectionsAndBackNavigation() {
        val context=ApplicationProvider.getApplicationContext<Context>(); val prefs=context.getSharedPreferences("glowsync",Context.MODE_PRIVATE); val previous=prefs.all.toMap()
        prefs.edit().putBoolean("setup",true).putString("host","127.0.0.1").commit()
        val ui=InstrumentationRegistry.getInstrumentation().uiAutomation
        fun find(node:AccessibilityNodeInfo?, label:String):AccessibilityNodeInfo? { if(node==null) return null; if(node.text?.toString()==label) return node; for(i in 0 until node.childCount) { val result=find(node.getChild(i),label); if(result!=null) return result }; return null }
        try { ActivityScenario.launch(MainActivity::class.java).use {
            var button:AccessibilityNodeInfo?=null; repeat(40) { if(button==null) { Thread.sleep(250); button=find(ui.rootInActiveWindow,"Calibration") } }
            assertNotNull("Calibration navigation missing",button); var target=button; while(target!=null && !target!!.isClickable) target=target!!.parent
            assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
            var shown=false; repeat(40) { if(!shown) { Thread.sleep(100); shown=find(ui.rootInActiveWindow,"Map your Ambilight")!=null } }; assertTrue("Calibration page missing",shown)
            ui.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            var home=false; repeat(40) { if(!home) { Thread.sleep(100); home=find(ui.rootInActiveWindow,"Your screen. Beyond its borders.")!=null } }; assertTrue("Back did not return home",home)
        } } finally { val edit=prefs.edit().clear(); previous.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Int -> edit.putInt(key,value); is Boolean -> edit.putBoolean(key,value); is Float -> edit.putFloat(key,value); is Long -> edit.putLong(key,value); is Set<*> -> edit.putStringSet(key,value.filterIsInstance<String>().toSet()) } }; edit.commit() }
    }
    @Test fun responseDiagnosticSendsReferencePattern() {
        val context=ApplicationProvider.getApplicationContext<Context>(); val prefs=context.getSharedPreferences("glowsync",Context.MODE_PRIVATE); val previous=prefs.all.toMap()
        prefs.edit().putBoolean("setup",true).putString("host","127.0.0.1").putInt("top",3).putInt("right",0).putInt("bottom",0).putInt("left",0).commit()
        val ui=InstrumentationRegistry.getInstrumentation().uiAutomation
        fun find(node:AccessibilityNodeInfo?, label:String):AccessibilityNodeInfo? { if(node==null) return null; if(node.text?.toString()==label) return node; for(i in 0 until node.childCount) { val result=find(node.getChild(i),label); if(result!=null) return result }; return null }
        fun scrollable(node:AccessibilityNodeInfo?):AccessibilityNodeInfo? { if(node==null) return null; if(node.isScrollable) return node; for(i in 0 until node.childCount) { val result=scrollable(node.getChild(i)); if(result!=null) return result }; return null }
        fun click(node:AccessibilityNodeInfo?) { var target=node; while(target!=null && !target!!.isClickable) target=target!!.parent; assertTrue("Could not click",target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true) }
        try { java.net.DatagramSocket(4048,java.net.InetAddress.getByName("127.0.0.1")).use { receiver -> receiver.soTimeout=10000
            ActivityScenario.launch(MainActivity::class.java).use {
                var settings:AccessibilityNodeInfo?=null; repeat(40) { if(settings==null) { Thread.sleep(250); settings=find(ui.rootInActiveWindow,"Settings") } }; assertNotNull(settings); click(settings)
                var open:AccessibilityNodeInfo?=null; repeat(20) { if(open==null) { Thread.sleep(200); open=find(ui.rootInActiveWindow,"Open response test"); if(open==null) scrollable(ui.rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) } }; assertNotNull("Response diagnostic missing",open); click(open)
                var trigger:AccessibilityNodeInfo?=null; repeat(40) { if(trigger==null || trigger?.isEnabled!=true) { Thread.sleep(250); trigger=find(ui.rootInActiveWindow,"Switch to white") } }; assertNotNull(trigger); click(trigger)
                val packet=java.net.DatagramPacket(ByteArray(1500),1500); receiver.receive(packet); assertEquals(19,packet.length); assertEquals(180,packet.data[10].toInt() and 255)
            }
        } } finally { val edit=prefs.edit().clear(); previous.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Int -> edit.putInt(key,value); is Boolean -> edit.putBoolean(key,value); is Float -> edit.putFloat(key,value); is Long -> edit.putLong(key,value); is Set<*> -> edit.putStringSet(key,value.filterIsInstance<String>().toSet()) } }; edit.commit() }
    }
    @Test fun missingConsentDoesNotStartCapture() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startService(Intent(it,ProjectionService::class.java)) }
            Thread.sleep(500)
            assertFalse(LiveStatus.active)
        }
        context.stopService(Intent(context,ProjectionService::class.java))
    }
    @Test fun controllerProfilesSurviveStoreRecreation() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("controller-test",Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val first=ControllerStore(prefs)
            first.save(WledDevice("Desk","192.168.1.2",30,"0.14"))
            first.save(WledDevice("TV","192.168.1.3",96,"0.15"))
            val restored=ControllerStore(prefs)
            assertEquals(2,restored.all().size)
            restored.remove("192.168.1.2")
            assertEquals("TV",ControllerStore(prefs).all().single().name)
        } finally { prefs.edit().clear().commit() }
    }
}
