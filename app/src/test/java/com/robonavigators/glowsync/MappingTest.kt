package com.robonavigators.glowsync
import com.robonavigators.glowsync.processing.*
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test
class MappingTest {
 @Test fun offsetAndReverseArePermutations() {
  val width=12; val height=8; val row=width*4; val b=ByteBuffer.allocate(row*height)
  for(y in 0 until height) for(x in 0 until width) { val p=y*row+x*4; b.put(p,(x*15).toByte()); b.put(p+1,(y*20).toByte()); b.put(p+2,30) }
  val base=EdgeSampler.sample(b,width,height,row,4,Layout(4,2,3,2),.1f)
  for(reverse in listOf(false,true)) for(offset in 0 until 11) {
   val out=EdgeSampler.sample(b,width,height,row,4,Layout(4,2,3,2,offset,reverse),.1f)
   for(k in 0 until 11) { val src=if(reverse) (11-k)%11 else k; val dst=(k+offset)%11; assertArrayEquals(base.copyOfRange(src*3,src*3+3),out.copyOfRange(dst*3,dst*3+3)) }
  }
 }
 @Test fun threeSidedProducesCorrectCount() { val data=ByteBuffer.allocate(16*4*8); assertEquals(27,EdgeSampler.sample(data,16,8,64,4,Layout(4,2,0,3)).size) }
 @Test(expected=IllegalArgumentException::class) fun rejectsEmptyLayout() { Layout(0,0,0,0) }
}
