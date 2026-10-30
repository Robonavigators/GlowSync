package com.robonavigators.glowsync.processing

import java.nio.ByteBuffer

data class ContentBounds(val left:Int,val top:Int,val right:Int,val bottom:Int)
/** Symmetric, conservative bars only. The same candidate must persist for several frames. */
class BlackBarDetector(private val holdFrames:Int=8) {
    private var current:ContentBounds?=null
    private var candidate:ContentBounds?=null
    private var votes=0
    fun detect(buf:ByteBuffer,w:Int,h:Int,row:Int,pixel:Int):ContentBounds {
        val full=ContentBounds(0,0,w,h)
        fun darkLine(index:Int,horizontal:Boolean):Boolean {
            val length=if(horizontal) w else h
            var bright=0; var samples=0
            for (i in 0 until length step maxOf(1,length/32)) {
                val x=if(horizontal) i else index; val y=if(horizontal) index else i
                val p=y*row+x*pixel
                if(p+2>=buf.limit()) return false
                val max=maxOf(buf.get(p).toInt() and 255,buf.get(p+1).toInt() and 255,buf.get(p+2).toInt() and 255)
                if(max>12) bright++; samples++
            }
            return bright <= samples/32
        }
        // Do not infer bars from a uniformly dark scene.
        var centerBright=0
        for(y in h/3 until 2*h/3 step maxOf(1,h/24)) for(x in w/3 until 2*w/3 step maxOf(1,w/24)) {
            val p=y*row+x*pixel
            if(p+2<buf.limit() && maxOf(buf.get(p).toInt() and 255,buf.get(p+1).toInt() and 255,buf.get(p+2).toInt() and 255)>24) centerBright++
        }
        fun symmetric(horizontal:Boolean):Int {
            val size=if(horizontal) h else w
            var a=0; var b=0
            while(a<size/4 && darkLine(a,horizontal)) a++
            while(b<size/4 && darkLine(size-1-b,horizontal)) b++
            return if(a>=2 && b>=2 && kotlin.math.abs(a-b)<=maxOf(2,size/40)) minOf(a,b) else 0
        }
        val proposal=if(centerBright<4) full else { val y=symmetric(true); val x=symmetric(false); ContentBounds(x,y,w-x,h-y) }
        val existing=current
        if(existing==null || existing.right>w || existing.bottom>h) { current=full; candidate=null; votes=0 }
        if(proposal==current) { candidate=null; votes=0 }
        else { if(candidate==proposal) votes++ else { candidate=proposal; votes=1 }; if(votes>=holdFrames) { current=proposal; candidate=null; votes=0 } }
        return current ?: full
    }
    fun reset() { current=null; candidate=null; votes=0 }
}
