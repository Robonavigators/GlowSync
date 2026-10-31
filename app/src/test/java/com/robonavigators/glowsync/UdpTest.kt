package com.robonavigators.glowsync
import com.robonavigators.glowsync.network.Ddp
import com.robonavigators.glowsync.network.DdpSender
import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
class UdpTest {
 @Test fun packetCanBeSentAndReceivedOnLoopback() {
  DatagramSocket(0,InetAddress.getLoopbackAddress()).use { receiver ->
   receiver.soTimeout=2000
   DatagramSocket().use { sender -> val bytes=Ddp.packets(byteArrayOf(1,2,3),1).single(); sender.send(DatagramPacket(bytes,bytes.size,InetAddress.getLoopbackAddress(),receiver.localPort)) }
   val packet=DatagramPacket(ByteArray(64),64); receiver.receive(packet)
   assertArrayEquals(byteArrayOf(0x41,1,11,1,0,0,0,0,0,3,1,2,3),packet.data.copyOf(packet.length))
  }
 }
 @Test fun invalidControllerAddressesAreRejectedBeforeUdpSenderCreation() {
  for(host in listOf("", " ", "http://wled.local", "wled.local:80", "wled.local/path")) {
   try {
    DdpSender(host).use { }
    fail("Accepted invalid controller address: $host")
   } catch (_: IllegalArgumentException) {
    // A malformed destination must fail before DNS resolution or UDP transmission.
   }
  }
 }
}
