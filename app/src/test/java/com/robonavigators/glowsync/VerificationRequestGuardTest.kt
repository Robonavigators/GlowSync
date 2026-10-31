package com.robonavigators.glowsync

import com.robonavigators.glowsync.network.VerificationRequestGuard
import org.junit.Assert.*
import org.junit.Test

class VerificationRequestGuardTest {
    @Test fun editsAndNewRequestsInvalidateEarlierResults() {
        val guard = VerificationRequestGuard()
        val first = guard.next()
        assertTrue(guard.isCurrent(first))
        guard.invalidate()
        assertFalse(guard.isCurrent(first))
        val second = guard.next()
        val third = guard.next()
        assertFalse(guard.isCurrent(second))
        assertTrue(guard.isCurrent(third))
    }
}
