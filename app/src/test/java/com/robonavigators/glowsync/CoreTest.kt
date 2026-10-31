package com.robonavigators.glowsync
import com.robonavigators.glowsync.network.Ddp
import com.robonavigators.glowsync.processing.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
class CoreTest {
 @Test fun headerAndSplit() { val p=Ddp.packets(ByteArray(1443){it.toByte()},7); assertEquals(2,p.size); assertArrayEquals(byteArrayOf(0x40,7,11,1,0,0,0,0,5,0xa0.toByte()),p[0].copyOfRange(0,10)); assertArrayEquals(byteArrayOf(0x41,7,11,1,0,0,5,0xa0.toByte(),0,3),p[1].copyOfRange(0,10)); assertEquals(0xa0,p[1][10].toInt() and 255) }
 @Test fun largeOffsetIsBigEndianAndPushOnlyOnLast() { val packets=Ddp.packets(ByteArray(69003),15); assertTrue(packets.size>45); assertArrayEquals(byteArrayOf(0,1,2,0xc0.toByte()),packets[46].copyOfRange(4,8)); assertEquals(0x40,packets[46][0].toInt() and 255); assertEquals(0x41,packets.last()[0].toInt() and 255) }
 @Test(expected=IllegalArgumentException::class) fun rejectBadChunk() { Ddp.packets(byteArrayOf(1,2,3),1,4) }
 @Test(expected=IllegalArgumentException::class) fun rejectEmpty() { Ddp.packets(byteArrayOf(),1) }
 @Test fun strideAndMapping() { val buf=ByteBuffer.allocate(32); for(y in 0..1) for(x in 0..1) { val p=y*16+x*4; buf.put(p,(x*100).toByte()); buf.put(p+1,(y*100).toByte()); buf.put(p+2,25) }; val out=EdgeSampler.sample(buf,2,2,16,4,Layout(2,0,2,0),.1f); assertArrayEquals(byteArrayOf(0,0,25,100,0,25,100,100,25,0,100,25),out) }
 @Test fun smoothingDependsOnTime() { val a=Smoother(100f); val b=Smoother(100f); a.apply(byteArrayOf(0),0f); b.apply(byteArrayOf(0),0f); a.apply(byteArrayOf(100),100f); b.apply(byteArrayOf(100),50f); val x=b.apply(byteArrayOf(100),50f); assertEquals(a.apply(byteArrayOf(100),0f)[0].toDouble(),x[0].toDouble(),1.0) }

 @Test fun exactMaximumPayloadIsOnePushedPacket() {
  val rgb=ByteArray(Ddp.MAX_PAYLOAD) { it.toByte() }
  val packets=Ddp.packets(rgb,1)
  assertEquals(1,packets.size)
  assertEquals(Ddp.MAX_PAYLOAD+10,packets.single().size)
  assertEquals(0x41,packets.single()[0].toInt() and 255)
  assertEquals(Ddp.MAX_PAYLOAD,((packets.single()[8].toInt() and 255) shl 8) or (packets.single()[9].toInt() and 255))
  assertArrayEquals(rgb,packets.single().copyOfRange(10,packets.single().size))
 }
 @Test fun customChunkUsesByteOffsetsAndOnlyFinalPush() {
  val rgb=ByteArray(15) { it.toByte() }
  val packets=Ddp.packets(rgb,2,6)
  assertEquals(3,packets.size)
  assertArrayEquals(byteArrayOf(0x40,2,11,1,0,0,0,0,0,6),packets[0].copyOfRange(0,10))
  assertArrayEquals(byteArrayOf(0x40,2,11,1,0,0,0,6,0,6),packets[1].copyOfRange(0,10))
  assertArrayEquals(byteArrayOf(0x41,2,11,1,0,0,0,12,0,3),packets[2].copyOfRange(0,10))
  assertArrayEquals(rgb,packets.flatMap { it.drop(10) }.toByteArray())
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsInsufficientPacketBuffer() {
  Ddp.writePacket(ByteArray(6),0,1,ByteArray(15))
 }
}
