# Tests

The Android project's tests remain alongside its app module:

- [`app/src/test`](../app/src/test): DDP encoding and loopback, mapping, colour processing, fallback, adaptive policies and diagnostic patterns.
- [`app/src/androidTest`](../app/src/androidTest): Android activity and capture lifecycle, consent, local DDP output, navigation, controller persistence and diagnostic output.

Run `./gradlew testDebugUnitTest lintDebug assembleDebug`. With a compatible attached device or emulator, run `./gradlew connectedDebugAndroidTest`. Consent-dialog tests use English system labels and may need adjustment across Android versions. An emulator test does not prove physical TV/WLED compatibility or visible LED latency.
