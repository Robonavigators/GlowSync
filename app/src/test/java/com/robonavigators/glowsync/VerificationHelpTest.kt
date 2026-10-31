package com.robonavigators.glowsync

import com.robonavigators.glowsync.network.verificationHelp
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationHelpTest {
    @Test fun failuresSuggestRelevantNextSteps() {
        assertTrue(verificationHelp(UnknownHostException()).contains("Address not found"))
        assertTrue(verificationHelp(SocketTimeoutException()).contains("timed out"))
        assertTrue(verificationHelp(ConnectException()).contains("power"))
        assertTrue(verificationHelp(IllegalArgumentException()).contains("without http://"))
        assertTrue(verificationHelp(Exception()).contains("web interface"))
    }
}
