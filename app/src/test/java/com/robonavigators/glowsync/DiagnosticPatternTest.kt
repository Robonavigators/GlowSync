package com.robonavigators.glowsync
import com.robonavigators.glowsync.diagnostics.DiagnosticPattern
import org.junit.Assert.*
import org.junit.Test
class DiagnosticPatternTest {
 @Test fun createsUniformWhiteAndBlackFrames() { assertArrayEquals(ByteArray(9){180.toByte()},DiagnosticPattern.frame(3,true)); assertArrayEquals(ByteArray(9),DiagnosticPattern.frame(3,false)) }
 @Test(expected=IllegalArgumentException::class) fun rejectsZeroLeds() { DiagnosticPattern.frame(0,true) }
}
