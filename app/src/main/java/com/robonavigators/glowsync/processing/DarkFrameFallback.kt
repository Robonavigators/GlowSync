package com.robonavigators.glowsync.processing

/** An all-dark capture may be legitimate or protected: never claim a cause. */
class DarkFrameFallback(private val mode: Mode, private val holdMs: Long = 2000) {
    enum class Mode { PASS_THROUGH, HOLD_THEN_DARK, STATIC }
    private var lastValid: ByteArray? = null
    private var darkSince: Long? = null
    fun apply(rgb: ByteArray, nowMs: Long): ByteArray {
        val dark = rgb.all { (it.toInt() and 255) <= 3 }
        if (!dark) { darkSince=null; lastValid=rgb.copyOf(); return rgb }
        val start=darkSince ?: nowMs.also { darkSince=it }
        return when(mode) {
            Mode.PASS_THROUGH -> rgb
            Mode.HOLD_THEN_DARK -> if(nowMs-start < holdMs && lastValid?.size == rgb.size) lastValid!!.copyOf() else rgb
            Mode.STATIC -> ByteArray(rgb.size) { index -> when(index%3) { 0 -> 24; 1 -> 18; else -> 10 } }
        }
    }
}
