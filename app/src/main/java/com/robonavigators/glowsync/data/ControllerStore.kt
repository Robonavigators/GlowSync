package com.robonavigators.glowsync.data

import android.content.SharedPreferences
import com.robonavigators.glowsync.network.WledDevice
import org.json.JSONArray
import org.json.JSONObject

/** Stores verified controller metadata locally; selection does not imply current reachability. */
class ControllerStore(private val prefs: SharedPreferences) {
    fun all(): List<WledDevice> = try {
        val array=JSONArray(prefs.getString("controllers","[]")); buildList {
            for(i in 0 until array.length()) { val j=array.getJSONObject(i); add(WledDevice(j.getString("name"),j.getString("host"),j.optInt("ledCount"),j.optString("version"))) }
        }
    } catch (_: Exception) { emptyList() }
    fun save(device: WledDevice) {
        val values=all().filterNot { it.host.equals(device.host,true) } + device
        val json=JSONArray(); values.forEach { d -> json.put(JSONObject().put("name",d.name).put("host",d.host).put("ledCount",d.ledCount).put("version",d.version)) }
        prefs.edit().putString("controllers",json.toString()).apply()
    }
    fun remove(host:String) {
        val json=JSONArray(); all().filterNot { it.host.equals(host,true) }.forEach { d -> json.put(JSONObject().put("name",d.name).put("host",d.host).put("ledCount",d.ledCount).put("version",d.version)) }
        prefs.edit().putString("controllers",json.toString()).apply()
    }
}
