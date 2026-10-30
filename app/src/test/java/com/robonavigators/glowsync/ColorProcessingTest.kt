package com.robonavigators.glowsync
import com.robonavigators.glowsync.processing.*
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test
class ColorProcessingTest {
 @Test fun brightnessAndDesaturation() { val rgb=byteArrayOf(100,50,0); ColorAdjust.apply(rgb,.5f,0f); assertEquals(rgb[0],rgb[1]); assertEquals(rgb[1],rgb[2]); assertTrue((rgb[0].toInt() and 255) in 25..35) }
 @Test fun barsNeedStableEvidenceAndDarkScenesRestoreFull() {
  val w=32; val h=24; val b=ByteBuffer.allocate(w*h*4)
  for(y in 4 until 20) for(x in 0 until w) { val p=(y*w+x)*4; b.put(p,100); b.put(p+1,100); b.put(p+2,100) }
  val d=BlackBarDetector(3); assertEquals(ContentBounds(0,0,w,h),d.detect(b,w,h,w*4,4)); d.detect(b,w,h,w*4,4); assertEquals(ContentBounds(0,4,w,20),d.detect(b,w,h,w*4,4))
  val dark=ByteBuffer.allocate(w*h*4); repeat(3) { d.detect(dark,w,h,w*4,4) }; assertEquals(ContentBounds(0,0,w,h),d.detect(dark,w,h,w*4,4))
 }
 @Test fun weightedSamplingFavorsTheEdge() { val b=ByteBuffer.allocate(4*4*4); for(x in 0 until 4) { b.put(x*4,100); b.put((4+x)*4,0) }; val layout=Layout(1,0,0,0); val average=EdgeSampler.sample(b,4,4,16,4,layout,.5f); val weighted=EdgeSampler.sample(b,4,4,16,4,layout,.5f,ContentBounds(0,0,4,4),true); assertTrue((weighted[0].toInt() and 255) > (average[0].toInt() and 255)) }
 @Test fun sideCorrectionFollowsPhysicalSideBeforeMapping() { val b=ByteBuffer.allocate(4*4*4); for(i in 0 until 16) { b.put(i*4,100); b.put(i*4+1,100); b.put(i*4+2,100) }; val gains=FloatArray(12){1f}; gains[0]=.5f; val out=EdgeSampler.sample(b,4,4,16,4,Layout(1,1,0,0),.1f,ContentBounds(0,0,4,4),false,gains); assertEquals(50,out[0].toInt() and 255); assertEquals(100,out[3].toInt() and 255) }
 @Test fun cropsBarFromSampling() { val w=4; val h=4; val b=ByteBuffer.allocate(w*h*4); for(y in 1..2) for(x in 0..3) b.put((y*w+x)*4,100); val rgb=EdgeSampler.sample(b,w,h,w*4,4,Layout(1,0,0,0),.1f,ContentBounds(0,1,4,3)); assertEquals(100,rgb[0].toInt() and 255) }
}
