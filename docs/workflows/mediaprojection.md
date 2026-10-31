# MediaProjection changes

Read [capture.md](../capture.md) and `app/src/main/java/com/robonavigators/glowsync/capture/ProjectionService.kt` first, then inspect `app/src/main/AndroidManifest.xml` and the instrumentation tests in `app/src/androidTest/java/com/robonavigators/glowsync/LifecycleTest.kt`.

**Lifecycle review order:** user consent → foreground-service start/type → projection callbacks → VirtualDisplay/ImageReader configuration → newest image acquisition → frame closure → resize/rotation → stop/revoke/sleep/process death → resource release. Verify cleanup on each failure branch.

- A new session requires genuine system consent; never cache/reuse tokens contrary to the installed Android version's rules.
- Respect secure windows/DRM, protected streaming apps and HDMI input limitations. Black frames are ambiguous; don't identify DRM solely by color.
- Close acquired images in all code paths; do not save, log, upload or leak pixel buffers.
- Keep capture/analysis off the main thread; discard old frames rather than growing a queue.
- Verify user stop and notification stop work consistently; ensure revocation cannot leave stale UDP output running.

**Validation:** run JVM tests, lint and debug build (`./gradlew testDebugUnitTest lintDebug assembleDebug`). With a compatible emulator/device, run `./gradlew connectedDebugAndroidTest` and verify consent denial, start/stop/restart, rotation/resize, background/resume and permission revocation; note that the standard Android CI workflow does **not** execute instrumentation tests. Record which steps were actually tested, since passing a JVM build does not establish lifecycle behavior. Physical TV testing is required for broad compatibility claims.
