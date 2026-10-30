package com.robonavigators.glowsync.processing

import java.nio.ByteBuffer
import kotlin.math.exp

/** Clockwise from top-left: top left-to-right, right top-to-bottom, bottom right-to-left, left bottom-to-top. */
data class Layout(val top: Int, val right: Int, val bottom: Int, val left: Int, val offset: Int = 0, val reverse: Boolean = false) {
    init { require(listOf(top,right,bottom,left).all { it in 0..1000 } && top+right+bottom+left in 1..2000) }
    val count get() = top+right+bottom+left
}
object EdgeSampler {
    fun sample(buf: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int, layout: Layout, depth: Float = .1f, bounds: ContentBounds = ContentBounds(0,0,width,height), weighted: Boolean = false, sideGain: FloatArray? = null): ByteArray {
        require(width > 0 && height > 0 && pixelStride >= 3 && rowStride >= width * pixelStride && depth in .01f..0.5f)
        require(bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width && bounds.bottom <= height && bounds.right > bounds.left && bounds.bottom > bounds.top)
        require(sideGain == null || (sideGain.size == 12 && sideGain.all { it in 0f..2f }))
        val cw=bounds.right-bounds.left; val ch=bounds.bottom-bounds.top
        val out = ByteArray(layout.count * 3); var i = 0
        fun edge(n: Int, side: Int) {
            repeat(n) { j ->
                var r=0L; var g=0L; var b=0L; var count=0
                val length = if (side % 2 == 0) cw else ch
                val thick = maxOf(1, ((if (side % 2 == 0) ch else cw) * depth).toInt())
                val start = j * length / n; val end = maxOf(start + 1, (j+1)*length/n).coerceAtMost(length)
                for (along in start until end step maxOf(1,(end-start)/8)) for (d in 0 until thick step maxOf(1,thick/8)) {
                    val x = bounds.left + when(side) { 0 -> along; 1 -> cw-1-d; 2 -> cw-1-along; else -> d }
                    val y = bounds.top + when(side) { 0 -> d; 1 -> along; 2 -> ch-1-d; else -> ch-1-along }
                    val pos = y*rowStride+x*pixelStride
                    if (pos+2 < buf.limit()) { val weight=if(weighted) thick-d else 1; r += (buf.get(pos).toInt() and 255)*weight; g += (buf.get(pos+1).toInt() and 255)*weight; b += (buf.get(pos+2).toInt() and 255)*weight; count += weight }
                }
                if (count > 0) { out[i*3]=((r.toFloat()/count)*(sideGain?.get(side*3) ?: 1f)).toInt().coerceIn(0,255).toByte(); out[i*3+1]=((g.toFloat()/count)*(sideGain?.get(side*3+1) ?: 1f)).toInt().coerceIn(0,255).toByte(); out[i*3+2]=((b.toFloat()/count)*(sideGain?.get(side*3+2) ?: 1f)).toInt().coerceIn(0,255).toByte() }; i++
            }
        }
        edge(layout.top,0); edge(layout.right,1); edge(layout.bottom,2); edge(layout.left,3)
        val mapped=ByteArray(out.size)
        for (k in 0 until layout.count) {
            val src = if (layout.reverse) (layout.count-k)%layout.count else k
            val dest = Math.floorMod(k+layout.offset,layout.count)
            out.copyInto(mapped,dest*3,src*3,src*3+3)
        }
        return mapped
    }
}
class Smoother(private val tauMs: Float) {
    private var state: FloatArray? = null
    fun apply(rgb: ByteArray, elapsedMs: Float): ByteArray {
        val s = state?.takeIf { it.size == rgb.size } ?: FloatArray(rgb.size) { (rgb[it].toInt() and 255).toFloat() }.also { state=it }
        val a = if (tauMs <= 0) 1f else 1f-exp(-elapsedMs.coerceAtLeast(0f)/tauMs)
        return ByteArray(rgb.size) { i -> s[i] += a*((rgb[i].toInt() and 255)-s[i]); s[i].toInt().coerceIn(0,255).toByte() }
    }
}
