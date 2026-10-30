# Guidance for coding agents

## Project

GlowSync is a native Kotlin and Jetpack Compose application for Android TV and Google TV. It analyzes permitted MediaProjection frames locally, samples TV edges, maps them to an RGB LED strip, and sends DDP over UDP to a WLED controller. The application ID is `com.robonavigators.glowsync`. Read `README.md` for current behavior, limitations, and build instructions; `PROMPT.md` contains the original product specification and is not a claim that every requested feature is complete.

## Engineering priorities

- Preserve a latest-frame-wins capture pipeline. Close each acquired image; do not introduce an unbounded queue, full-resolution per-frame bitmaps, compression, or streaming HTTP calls.
- Keep capture, processing, discovery, and UDP transmission off the UI thread. Serialize packets to each destination and do not send stale frames to satisfy a frame rate.
- Follow Android's MediaProjection consent, foreground-service, callback, and token lifecycle requirements. Never bypass secure windows or DRM, and never claim protected content or HDMI inputs are capturable when the platform does not permit it.
- Treat UDP sends and HTTP verification accurately: neither confirms physical LED output. Use monotonic timing for app measurements, and do not present a UDP call duration as end-to-end latency.
- Preserve local-only frame processing. Do not upload, record, or log captured frames. Explain unencrypted WLED HTTP/DDP traffic and use trusted local networks.
- Keep controls usable with a TV remote, visible focus, adequate contrast, and reduced-motion settings. Avoid expensive effects on the capture path.
- Prefer small, well-scoped changes; maintain the existing MIT license and update `README.md` whenever behavior or limitations change.

## Layout and validation

- `app/src/main/java/com/robonavigators/glowsync/capture`: projection service and capture lifecycle.
- `processing`: edge sampling, colour adjustment, smoothing, and fallback logic.
- `network`: WLED verification/discovery and DDP encoding/transmission.
- `performance`: adaptive rate and analysis-resolution policies.
- `diagnostics`: direct-UDP test patterns; these bypass screen capture.
- `app/src/test` and `app/src/androidTest`: JVM and Android instrumentation tests.

Use Android SDK 35 and JDK 17 or newer. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` and, when a compatible device or emulator is available, `./gradlew connectedDebugAndroidTest`. Capture-consent instrumentation uses English system-dialog labels and may need adaptation for other Android versions or locales. Report actual results and distinguish emulator tests from physical TV/WLED validation. Do not claim physical latency, reliability, or production readiness without evidence.
