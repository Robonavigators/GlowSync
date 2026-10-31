# Frame processing and low-latency workflow

Read [performance.md](../performance.md), [architecture](../architecture.md) and `performance/AdaptiveRate.kt`, `performance/ResolutionPolicy.kt`, `processing/EdgeSampler.kt`, `capture/ProjectionService.kt`.

1. Define a measurement: capture-to-process, process-to-DDP encode, UDP send-call time, frame age or separately **physical screen-to-light**. Never collapse these into one number.
2. Establish a baseline with device/build, frame size, LED count, frame-rate target, network and measurement tool. When physical hardware is absent, label results synthetic/emulated.
3. Maintain newest-frame-only pending work, bounded memory, immediate image closure, no full-screen bitmap per frame and no UI-thread processing.
4. Inspect allocations, recomposition, worker contention, backlog, UDP packet count and network error handling before altering thread assignments.
5. Change one performance variable at a time and compare repeatable before/after measurements. Preserve Eco/Balanced/Fast/Ultra as adaptive targets, not guarantees.
6. Run `AdaptiveRateTest.kt`, `ResolutionPolicyTest.kt` and relevant processing tests plus lint/build. Retest actual controller behavior if available.
7. Document verified improvements with measurement method and variance; don't invent millisecond or FPS performance.

**Stop:** if optimization disables consent, hides errors, leaks images or needs unsupported platform privileges.
