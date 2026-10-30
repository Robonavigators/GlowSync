package com.robonavigators.glowsync
import com.robonavigators.glowsync.performance.AdaptiveRate
import org.junit.Assert.*
import org.junit.Test
class AdaptiveRateTest {
 @Test fun reducesOnSustainedOverloadAndRecoversSlowly() {
  val rate=AdaptiveRate(30); repeat(4) { rate.observe(40.0, 1.0, false) }; assertEquals(30,rate.fps)
  rate.observe(40.0,1.0,false); assertEquals(24,rate.fps)
  repeat(240) { rate.observe(1.0,1.0,false) }; assertEquals(26,rate.fps)
 }
 @Test fun replacementTriggersReduction() { val rate=AdaptiveRate(15); repeat(5) { rate.observe(0.0,0.0,true) }; assertEquals(12,rate.fps) }
}
