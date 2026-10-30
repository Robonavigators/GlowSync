package com.robonavigators.glowsync
import com.robonavigators.glowsync.performance.ResolutionPolicy
import org.junit.Assert.*
import org.junit.Test
class ResolutionPolicyTest {
 @Test fun changesAreTimeBoundedAndRecoverConservatively() { val p=ResolutionPolicy(60,0); assertEquals(480,p.width); assertNull(p.observe(30,4999)); assertEquals(320,p.observe(30,5000)); assertEquals(192,p.observe(30,10000)); assertNull(p.observe(60,39999)); assertEquals(320,p.observe(60,40000)); assertEquals(480,p.observe(60,70000)) }
 @Test fun ecoNeverDropsBelowMinimum() { val p=ResolutionPolicy(15,0); assertNull(p.observe(10,10000)); assertEquals(192,p.width) }
}
