package com.robonavigators.glowsync.performance

/** Conservative controller: lower the output cap when work cannot sustain it; recover slowly. */
class AdaptiveRate(val ceiling: Int) {
    init { require(ceiling in 10..60) }
    var fps: Int = ceiling
        private set
    private var busy = 0
    private var healthy = 0
    fun observe(processingMs: Double, sendMs: Double, replaced: Boolean) {
        val overloaded = replaced || processingMs + sendMs > 1000.0 / fps * 0.8
        if (overloaded) { busy++; healthy = 0 } else { healthy++; busy = 0 }
        if (busy >= 5) { fps = (fps * 0.8).toInt().coerceAtLeast(10); busy = 0 }
        if (healthy >= fps * 10) { fps = (fps + 2).coerceAtMost(ceiling); healthy = 0 }
    }
}
