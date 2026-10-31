package com.robonavigators.glowsync.network

/** Tracks requests on the UI thread so older results cannot replace a newer selection. */
class VerificationRequestGuard {
    private var generation = 0L
    fun next(): Long = ++generation
    fun invalidate() { generation++ }
    fun isCurrent(request: Long): Boolean = request == generation
}
