package com.robonavigators.glowsync.performance

/** Changes cautiously; timing is monotonic and independent of frame rate. */
class ResolutionPolicy(private val ceiling:Int, nowMs:Long) {
    val preferred = when { ceiling <= 15 -> 192; ceiling <= 30 -> 320; else -> 480 }
    var width = preferred
        private set
    private var lastChange=nowMs
    fun observe(rate:Int, nowMs:Long): Int? {
        if (rate < ceiling * .8 && width > 192 && nowMs-lastChange >= 5000) {
            width=if(width>320) 320 else 192; lastChange=nowMs; return width
        }
        if (rate >= ceiling * .95 && width < preferred && nowMs-lastChange >= 30000) {
            width=if(width<320) 320.coerceAtMost(preferred) else preferred; lastChange=nowMs; return width
        }
        return null
    }
}
