# Bug-fix workflow

1. Capture expected vs actual behavior, reproduction steps, device/Android/WLED versions (if applicable), whether emulator or actual hardware, and a sanitized error trace. Never request recorded screen contents.
2. Inspect the failing call path and relevant tests. Check [architecture](../architecture.md), [troubleshooting](../TROUBLESHOOTING.md), and [do/don't](../DOS-AND-DONTS.md).
3. Write a failing regression test when possible before changing production code; isolate the root cause instead of masking it with retries, catch-all exceptions or fixed sleeps.
4. Change the smallest responsible method. Maintain capture cleanup, network backoff, latest-frame-wins and remote navigation.
5. Run the focused JVM test and then `python3 -m unittest discover -s tests -v` plus `./gradlew testDebugUnitTest lintDebug assembleDebug`. Use `./gradlew connectedDebugAndroidTest` only when a compatible device/emulator exists.
6. Update README or relevant docs if behavior or limitations changed. Report actual test output, remaining risk and tested environment.

**Stop:** if reproducing would require DRM bypass, hidden permissions, captured private data or unsafe device modifications. Ask for a sanitized alternative; never claim a fix without checks.
