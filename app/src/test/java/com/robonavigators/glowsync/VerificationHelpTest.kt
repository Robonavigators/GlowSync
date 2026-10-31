package com.robonavigators.glowsync

import com.robonavigators.glowsync.network.verificationHelp
import com.robonavigators.glowsync.network.validateWledHost
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VerificationHelpTest {
    @Test fun failuresSuggestRelevantNextSteps() {
        assertTrue(verificationHelp(UnknownHostException()).contains("Address not found"))
        assertTrue(verificationHelp(SocketTimeoutException()).contains("timed out"))
        assertTrue(verificationHelp(ConnectException()).contains("power"))
        assertTrue(verificationHelp(IllegalArgumentException()).contains("without http://"))
        assertTrue(verificationHelp(Exception()).contains("web interface"))
    }

    @Test fun acceptsPlainControllerAddresses() {
        listOf("192.168.0.42", "wled.local", "WLED-ABC.local", "my_controller.local")
            .forEach(::validateWledHost)
    }

    @Test fun rejectsUrlComponentsAndWhitespaceInControllerAddresses() {
        for (host in listOf(
            "", " ", "http://wled.local", "wled.local:80", "wled.local/path",
            "wled.local?wrong", "wled.local#fragment", "wled.local\\other",
            "wled.local\n", "wled.local%2F"
        )) {
            try {
                validateWledHost(host)
                fail("Accepted invalid controller address: $host")
            } catch (_: IllegalArgumentException) {
                // Expected: verification must not construct a URL from these inputs.
            }
        }
    }
}
