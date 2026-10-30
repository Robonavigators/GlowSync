package com.robonavigators.glowsync
import com.robonavigators.glowsync.processing.DarkFrameFallback
import org.junit.Assert.*
import org.junit.Test
class FallbackTest {
 @Test fun holdIsBoundedAndRecovers() { val f=DarkFrameFallback(DarkFrameFallback.Mode.HOLD_THEN_DARK,1000); val red=byteArrayOf(80,0,0); val dark=ByteArray(3); f.apply(red,0); assertArrayEquals(red,f.apply(dark,1)); assertArrayEquals(dark,f.apply(dark,1001)); assertArrayEquals(red,f.apply(red,1100)) }
 @Test fun staticOnlyForDarkFrames() { val f=DarkFrameFallback(DarkFrameFallback.Mode.STATIC); assertArrayEquals(byteArrayOf(24,18,10),f.apply(ByteArray(3),0)); val rgb=byteArrayOf(0,4,0); assertArrayEquals(rgb,f.apply(rgb,10)) }
 @Test fun passThroughDoesNotAlterDarkScenes() { val f=DarkFrameFallback(DarkFrameFallback.Mode.PASS_THROUGH); val dark=byteArrayOf(1,2,3); assertArrayEquals(dark,f.apply(dark,0)) }
}
