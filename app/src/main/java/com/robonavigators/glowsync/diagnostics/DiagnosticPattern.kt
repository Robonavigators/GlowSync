package com.robonavigators.glowsync.diagnostics

/** A uniform RGB reference for an external camera or photodiode measurement. */
object DiagnosticPattern {
    fun frame(count:Int, illuminated:Boolean):ByteArray {
        require(count in 1..2000)
        return ByteArray(count*3) { if(illuminated) 180.toByte() else 0 }
    }
}
