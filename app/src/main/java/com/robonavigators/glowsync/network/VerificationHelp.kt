package com.robonavigators.glowsync.network

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Gives testers actionable guidance without exposing internal exception names. */
fun verificationHelp(error: Exception): String = when (error) {
    is UnknownHostException -> "Address not found. Check the IP address or hostname and your local network."
    is SocketTimeoutException -> "Connection timed out. Check that the TV and WLED are on the same reachable network."
    is ConnectException -> "Could not reach WLED. Check its address, power, and network connection."
    is IllegalArgumentException -> "Check the address (without http:// or a port) and confirm the device is running WLED."
    else -> "Could not verify WLED. Check its address and open its web interface from another device."
}

/** Reject URL syntax before constructing the fixed /json/info verification endpoint. */
fun validateWledHost(host: String) {
    require(host.isNotBlank() && host.none {
        it.isWhitespace() || it.code < 32 || it.code == 127 || it in "/\\@:#?%"
    }) { "Enter a plain IP address or hostname without URL components" }
}
