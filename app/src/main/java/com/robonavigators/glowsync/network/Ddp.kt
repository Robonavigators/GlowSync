package com.robonavigators.glowsync.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** RGB24 DDP; offsets are channel bytes and only the final packet pushes the frame. */
object Ddp {
    const val MAX_PAYLOAD=1440
    fun writePacket(rgb:ByteArray, offset:Int, sequence:Int, out:ByteArray, chunkSize:Int=MAX_PAYLOAD): Int {
        require(rgb.isNotEmpty() && rgb.size%3==0 && offset in rgb.indices && offset%3==0)
        require(chunkSize in 3..MAX_PAYLOAD && chunkSize%3==0 && sequence in 0..15)
        val n=minOf(chunkSize,rgb.size-offset)
        require(out.size>=n+10)
        out[0]=(0x40 or if(offset+n==rgb.size) 1 else 0).toByte()
        out[1]=sequence.toByte(); out[2]=0x0b; out[3]=1
        out[4]=(offset ushr 24).toByte(); out[5]=(offset ushr 16).toByte(); out[6]=(offset ushr 8).toByte(); out[7]=offset.toByte()
        out[8]=(n ushr 8).toByte(); out[9]=n.toByte()
        rgb.copyInto(out,10,offset,offset+n)
        return n+10
    }
    fun packets(rgb: ByteArray, sequence: Int, chunkSize: Int = MAX_PAYLOAD): List<ByteArray> {
        require(rgb.isNotEmpty() && rgb.size%3==0 && chunkSize in 3..MAX_PAYLOAD && chunkSize%3==0 && sequence in 0..15)
        return (rgb.indices step chunkSize).map { offset ->
            val buffer=ByteArray(10+minOf(chunkSize,rgb.size-offset)); writePacket(rgb,offset,sequence,buffer,chunkSize); buffer
        }
    }
}
data class SendTiming(val encodeNanos:Long,val sendNanos:Long)
class DdpSender(host: String): AutoCloseable {
    // Validate before DNS resolution so blank or URL-like values cannot become UDP targets.
    init { validateWledHost(host) }
    private val address = InetAddress.getByName(host)
    private val socket = DatagramSocket()
    private val buffer=ByteArray(10+Ddp.MAX_PAYLOAD)
    private val packet=DatagramPacket(buffer,buffer.size,address,4048)
    private var seq = 0
    @Synchronized fun send(rgb: ByteArray): SendTiming {
        require(rgb.isNotEmpty() && rgb.size%3==0)
        seq = seq % 15 + 1
        var encode=0L; var sending=0L
        for(offset in rgb.indices step Ddp.MAX_PAYLOAD) {
            val start=System.nanoTime(); val length=Ddp.writePacket(rgb,offset,seq,buffer); packet.setData(buffer,0,length); val ready=System.nanoTime(); socket.send(packet); val end=System.nanoTime()
            encode+=ready-start; sending+=end-ready
        }
        return SendTiming(encode,sending)
    }
    override fun close() = socket.close()
}
