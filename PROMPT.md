# GLOWSYNC — PRODUCTION-GRADE GOOGLE TV AMBILIGHT APPLICATION

## PROJECT IDENTITY

**App name:** GlowSync  
**Developer:** Robonavigators  
**Branding:** GlowSync — By Robonavigators  
**Platform:** Android TV / Google TV  
**Application ID:** `com.robonavigators.glowsync`  
**Language:** Kotlin  
**UI:** Jetpack Compose with TV-specific navigation  
**Lighting backend:** WLED on ESP32/ESP8266  
**Primary streaming protocol:** DDP over UDP  
**Core priority:** Ultra-low latency, stability, smoothness, and premium design.

You are acting as a senior Android platform engineer, graphics engineer, networking specialist, embedded systems engineer, and premium UI/UX designer.

Build a **real, complete, compilable Android Studio project** for GlowSync.

Do not create a web app, WebView wrapper, static mockup, or proof-of-concept-only implementation.

The app must capture permitted Android TV screen content, extract colours corresponding to physical LED positions around the TV, and transmit individual RGB values to WLED over the local network.

It must have a stunning, animated glassmorphism interface, professionally designed specifically for television screens and remote controls.

Implement working functionality, tests, documentation, and a buildable APK.

---

# 1. NON-NEGOTIABLE ENGINEERING PRIORITIES

Priority order:

1. Minimum achievable screen-to-LED latency.
2. Accurate LED mapping and responsive colour reproduction.
3. Reliable Android screen capture and foreground operation.
4. Stable WLED networking and recovery.
5. Low CPU/GPU overhead.
6. Premium 60 FPS-capable TV interface.
7. User privacy, transparency and Android compliance.
8. Clean architecture and maintainable source code.

Never sacrifice capture performance for decorative animations.

Never claim that protected streaming content can be captured if Android blocks it.

Never fake WLED connectivity, capture status, FPS, or performance measurements.

The application must perform genuine screen capture and send genuine per-LED data.

---

# 2. ULTRA-LOW-LATENCY ENGINE

This is the most important component of GlowSync.

## Performance objectives

Target the following on capable hardware and networks:

- Adaptive screen processing: 10–60 FPS.
- Preferred fast mode: 30–60 FPS when sustainable.
- Balanced mode: approximately 20–30 FPS.
- Eco mode: approximately 10–15 FPS.
- Aspirational capture-to-UDP processing latency: under 25 ms.
- Aspirational screen-to-visible-LED latency: under 80 ms.
- No growing processing queues.
- No stale-frame transmission.
- No unnecessary colour-transition delays.

These are performance targets, not guarantees. Report actual measurements.

## Pipeline architecture

Implement:

MediaProjection → VirtualDisplay → Low-resolution frame surface → Edge sampling → Colour processing → LED mapping → DDP packet encoding → UDP transmission → WLED.

Separate these responsibilities into dedicated, testable components.

Use a latest-frame-wins design.

### Critical rules

- Use `ImageReader.acquireLatestImage()` where appropriate.
- Close every acquired image reliably.
- Do not use `acquireNextImage()` in a way that creates frame backlog.
- Avoid creating full-resolution Bitmap objects per frame.
- Avoid JPEG/PNG encoding or video compression.
- Avoid HTTP requests for continuous colour streaming.
- Avoid allocating large temporary objects on every frame.
- Never put the capture, pixel-processing, and network pipeline on the Compose UI thread.
- Never transmit old frames merely to maintain a target FPS.
- Keep only the newest eligible frame when processing falls behind.
- Avoid blocking the capture callback on networking.
- Reuse frame-processing and packet buffers wherever safe.
- Use monotonic timestamps for measurement.

## Capture resolution

Start with a low-resolution analysis target such as 320×180 or 640×360.

Preserve the correct aspect ratio.

Use lower resolutions for weak television processors and increase resolution only if measurement demonstrates a meaningful quality benefit.

Do not process a 4K image when a much smaller sampling image produces equivalent lighting information.

Investigate efficient GPU-assisted downsampling with OpenGL ES when it offers a measurable improvement.

Provide a simpler and reliable CPU sampling path as fallback.

Do not introduce GPU-to-CPU readback stalls without benchmarking.

## Threading strategy

Use dedicated workers for:

- Android capture events.
- Edge-colour processing.
- LED packet encoding and transmission.
- Network discovery and connection management.
- User interface.

Use Kotlin coroutines and bounded or conflated channels where appropriate.

Prevent thread contention and excessive context switching.

If a frame arrives while the previous frame is still processing, discard obsolete work safely.

Use a single serialized transmitter per WLED destination so multi-packet LED frames cannot interleave incorrectly.

---

# 3. COLOUR EXTRACTION ENGINE

Build a sophisticated, efficient Ambilight engine.

For every LED, assign a sampling region corresponding to its physical position around the TV.

Support independent LED counts for:

- Top
- Bottom
- Left
- Right

Support different starting corners and clockwise/counterclockwise strip direction.

## Sampling algorithms

Implement:

1. Fast average colour sampling.
2. Weighted edge sampling.
3. Optional vibrant colour mode.
4. Cinematic natural colour mode.
5. Adjustable colour saturation.
6. Adjustable brightness.
7. Configurable edge sampling depth.
8. Optional black-bar detection.
9. Optional per-side colour correction.

Avoid allowing bright subtitles or small isolated highlights to dominate an entire wall unnecessarily.

For black-bar detection, use a stable, conservative algorithm with hysteresis so sampling regions do not constantly jump around during dark scenes.

Handle letterboxing, pillarboxing, and capture-region size changes.

## Colour smoothing

Implement time-based adaptive smoothing rather than fixed delays.

Available profiles:

**Ultra Fast**
- Minimal smoothing.
- Respond as quickly as possible.
- Ideal for fast-moving gameplay.

**Balanced**
- Minor flicker reduction.
- Maintains good responsiveness.

**Cinematic**
- Smooth colour transitions.
- Suitable for movies and calm scenes.

**Custom**
- Configurable smoothing time constant.
- Sensitivity, saturation and brightness controls.

Use elapsed time in smoothing calculations to avoid different behaviour at different frame rates.

When smoothing is disabled, do not intentionally delay colour changes.

---

# 4. WLED REAL-TIME COMMUNICATION

Use WLED's Distributed Display Protocol (DDP) as the main transport.

## Network settings

- Default DDP destination port: UDP 4048.
- Use unicast to the selected device.
- Support RGB24 initially.
- Investigate RGBW only when supported and appropriate.
- Support configurable output FPS.
- Support multiple DDP packets for larger LED installations.

Implement the actual DDP protocol correctly.

Follow WLED's current DDP implementation and documentation.

### DDP packet handling

Use the proper 10-byte header and fields, including:

- Protocol flags and version.
- Sequence number.
- RGB24 data type.
- Display destination ID.
- Byte-based channel offset.
- Payload length.
- Final-packet PUSH flag.

Split payloads into appropriately sized chunks, normally no more than 1440 RGB data bytes per packet to avoid IP fragmentation on typical networks.

Encode channel offsets and lengths correctly.

Only mark the final packet of a complete frame for display where required by the protocol.

Unit-test the packet generator using independently verified reference examples.

## Networking optimizations

- Keep a UDP socket available throughout active streaming.
- Avoid DNS resolution on every frame.
- Reuse output buffers.
- Avoid per-frame HTTP JSON calls.
- Prevent old-frame packet backlog.
- Apply configurable FPS limiting if the ESP controller cannot keep up.
- Track network exceptions and connection failures.
- Use exponential backoff for control-plane reconnection.
- Do not place reconnection delays on the active capture thread.
- Do not invent packet-delivery acknowledgements for ordinary UDP.
- Support static IP and hostname configuration.

Implement DDP as the primary protocol and consider WLED-compatible fallbacks only when actually needed.

## WLED discovery

Use Android-supported local service discovery where available.

Provide:

- Discover nearby WLED controllers.
- Device name.
- IP address.
- Connection status.
- Reported LED count.
- Firmware information when available.
- Manual IP entry.
- Test connection.
- Save controller.
- Reconnect to a saved device.

Use WLED's `/json/info` and relevant JSON APIs for discovery verification and configuration information.

Do not repeatedly poll HTTP status during the high-speed streaming loop.

Support multiple saved controllers, but fully optimize single-controller streaming first.

---

# 5. ANDROID SCREEN CAPTURE AND PERMISSIONS

Use the official Android `MediaProjection` API.

The app must launch the genuine Android system capture-permission dialog.

## Permission workflow

Design an elegant introductory screen:

**Enable Screen Capture**

Description:
"GlowSync analyzes permitted screen colours locally to synchronize your ambient lighting."

Primary action:
"Allow Screen Capture"

When selected, call the appropriate Android `MediaProjectionManager` API.

Do not show a fake permission dialog.

Do not bypass or suppress Android's system permission screens.

On compatible Android versions, prefer a capture configuration for the default display when appropriate for TV-wide capture.

Respect device and OS limitations.

## Foreground service

Implement a properly declared media-projection foreground service.

Include required manifest permissions and service types for the supported Android versions.

Handle the correct order of consent, foreground-service startup, and projection acquisition.

For Android 14+, ensure:

- Fresh user consent for each new projection session.
- Each projection token is used correctly.
- `MediaProjection.Callback` is registered.
- One `createVirtualDisplay()` per authorized projection session.
- Proper cleanup when capture stops.
- Appropriate resize handling without reusing invalid tokens.

Support continuing an active permitted capture session when the user returns to the TV home screen or opens another app, where the television permits this.

Do not promise permanent silent capture permission.

Do not silently restart a media projection after reboot when prohibited.

Never use root, insecure workarounds, or Accessibility Services to bypass platform restrictions.

## Local network permission

Handle Android's version-specific local networking permissions.

Include Android 17 `ACCESS_LOCAL_NETWORK` handling when targeting API 37+, using the appropriate system permission flow.

For lower target SDKs, follow the correct corresponding requirements.

Do not request permissions unnecessarily.

## Protected video

Correctly handle:

- DRM-protected streaming content.
- `FLAG_SECURE` windows.
- Black capture frames.
- Unsupported TV firmware.
- HDMI inputs that are inaccessible to normal Android applications.
- Revoked capture permission.

Never try to bypass DRM or secure display protections.

If content is unavailable, show an honest status and use a configurable fallback such as static ambient lighting, fade-out, or last valid colour with a short timeout.

---

# 6. PREMIUM GLASSMORPHISM DESIGN SYSTEM

The UI must feel like a luxury commercial television application.

Visual inspirations:

- Premium Apple TV-style fluidity.
- Modern cinematic home-theater interfaces.
- Frosted glass.
- Subtle futuristic lighting.
- Clean, spacious controls.
- Elegant interactive animations.

Do not copy other applications' assets or proprietary designs.

## Brand styling

App name: GlowSync

Brand credit:
**By Robonavigators**

Colours:

- Midnight navy: `#080F1E`
- Deep blue: `#101B32`
- Sapphire accent: `#4DA8FF`
- Cyan highlights: `#74D5FF`
- Soft violet: `#9369F8`
- Frosted white: `#EAF3FF`

Use accessible text contrast.

## Visual effects

- Translucent glass panels.
- Subtle blur where GPU performance allows.
- Fine translucent borders.
- Layered background gradients.
- Animated ambient light halos.
- Soft interactive focus glows.
- Smooth opacity and scale transitions.
- Minimal, sophisticated shadows.
- Refined animated progress indicators.

Use Compose animations and appropriate graphics APIs.

Do not implement expensive full-screen real-time blurs on weak hardware.

Prefer static or cached blurred assets where suitable.

Target fluid 60 FPS UI interactions on capable devices.

Always prioritize input responsiveness.

## TV remote support

Implement proper D-pad navigation:

- Up
- Down
- Left
- Right
- Select
- Back

Every interactive element must be focusable and have a visible focus state.

Use larger TV-friendly hit targets.

Provide predictable focus restoration between pages.

Avoid mouse-dependent controls or mobile-only layouts.

Respect reduced-motion accessibility preferences.

---

# 7. CINEMATIC ANIMATED SPLASH SCREEN

On app launch:

1. Show a beautiful midnight-blue gradient background.
2. Animate a soft blue light illuminating the center.
3. Reveal the GlowSync icon.
4. Smoothly reveal the text "GlowSync".
5. Fade in "By Robonavigators".
6. Transition naturally into the dashboard or first-time setup.

Do not deliberately delay startup for a long animation.

A returning user should be able to reach the dashboard quickly.

Design an original vector app icon and Android TV banner.

Use optimized assets to keep APK size and startup costs reasonable.

---

# 8. FIRST-TIME SETUP WIZARD

Build an immersive setup wizard with animated progress and persistent state.

Every step should have a cinematic glass panel, clear description, visual illustration and TV-remote navigation.

## Step 1 — Welcome

Title:
"Your Screen. Beyond Its Borders."

Description:
"Bring every supported scene to life with immersive ambient lighting."

Show an animated television with simulated reactive LEDs.

Button:
"Get Started"

## Step 2 — Discover WLED

Title:
"Connect Your Lights"

Show a beautiful animated network scanning visualization.

Discover nearby WLED devices where possible.

Display each discovered controller in a frosted glass card.

Allow:
- Refresh devices.
- Select device.
- Enter IP manually.
- Test connection.

Do not label a device connected until real communication confirms it.

## Step 3 — LED Configuration

Title:
"Map Your Ambilight"

Show an interactive television diagram.

Allow users to choose:

- Three-sided or four-sided arrangement.
- LED count per edge.
- Starting corner.
- Strip direction.
- Physical LED ordering.
- LED offset and reversal.
- LED type where relevant.

Animate the path of the LEDs around the virtual television.

Provide a real LED test function that sends distinct colours or a moving marker to the physical strip.

Allow a one-LED-at-a-time direction test.

Verify mapping changes against the WLED output where possible.

## Step 4 — Capture Permission

Title:
"Bring Your Screen to Life"

Explain why screen capture is needed.

Button:
"Allow & Continue"

Trigger the real Android system screen-capture permission dialog.

Handle acceptance, denial, and unsupported devices.

Never fake success.

## Step 5 — Live Calibration

Title:
"Make Every Colour Perfect"

Show a simulated TV preview and, when capture is authorized and permitted, a genuine live LED colour preview.

Include:

- Brightness.
- Saturation.
- White balance.
- Edge depth.
- Smoothing.
- Processing FPS.
- Colour mode.
- Black-bar detection.

Show a basic latency/response diagnostic using test-pattern transitions where possible.

Do not expose protected captured frame data in logs or persistent storage.

## Step 6 — Setup Complete

Title:
"Your GlowSync Is Ready"

Use a subtle animated success effect.

Show actual verified setup status:

- WLED connection.
- LED count.
- Mapping.
- Capture authorization/session state.
- Performance mode.

Button:
"Enter GlowSync"

Persist completed configuration and skip setup on subsequent launches unless reset is requested.

---

# 9. MAIN DASHBOARD

Create an extremely polished dashboard.

## Header

GlowSync logo and name.

Small subtitle:
"By Robonavigators"

Connection status and settings shortcut.

## Hero section

Show a stylized television with animated ambient light around its edges.

When active, the preview should react to the actual computed LED colour array.

When capture is inactive, clearly label the animation as a preview.

Avoid reconstructing an unnecessary full captured screen image.

## Controls

Prominent:

**Start Sync / Stop Sync**

Additional controls:

- Lighting mode.
- Brightness.
- Colour saturation.
- Smoothing profile.
- WLED device.
- LED calibration.
- Performance mode.

## Dashboard metrics

Display genuine data when available:

- Capture FPS.
- Frames analyzed.
- Frames dropped.
- DDP output FPS.
- Processing latency.
- Network transmission status.
- WLED controller connection state.

Do not misrepresent the timestamp of the last UDP send as confirmed physical LED output.

Provide an advanced diagnostics panel without cluttering the main UI.

---

# 10. ADVANCED PERFORMANCE PROFILES

Implement these profiles with measured adaptive behaviour.

### Eco

Low processing overhead.

Target approximately 10–15 FPS.

Lower analysis resolution.

### Balanced

Default for first-time users.

Target approximately 20–30 FPS.

Moderate smoothing and low CPU consumption.

### Fast

Target approximately 30 FPS or better.

Minimal smoothing.

Optimized sampling.

### Ultra

Attempt up to 60 FPS only if capture, television CPU/GPU, network and WLED hardware can sustainably support it.

Prioritize freshness over throughput.

Automatically reduce FPS if excessive frame drops, processing time or resource contention occur.

### Custom

Allow configuration of:

- Capture analysis resolution.
- Output FPS cap.
- Colour smoothing.
- Edge sampling depth.
- Saturation.
- Brightness.
- Black-bar handling.
- Network packet behaviour.

Show a clear warning that 60 FPS may not be available on all televisions or LED controllers.

---

# 11. AUTOMATIC PERFORMANCE OPTIMIZATION

Build an adaptive performance manager.

Track:

- Effective capture FPS.
- Capture callback timing.
- Frame-processing duration.
- Packet encoding duration.
- UDP transmission-call duration.
- Dropped-frame count.
- Frame age when dispatched.
- CPU usage where available.
- Performance trends over time.

Use these signals to change analysis resolution or output FPS when necessary.

Do not estimate end-to-end visible LED latency merely from a UDP send call.

For actual physical end-to-end measurements, provide a documented external measurement procedure using a high-speed camera or photodiode.

Display separately:

1. Measured app processing latency.
2. Measured or estimated transport information.
3. Externally measured screen-to-LED latency, if supplied.

Never fabricate benchmark results.

---

# 12. BACKGROUND OPERATION

When the user starts GlowSync and accepts capture permission:

- Start the foreground service correctly.
- Continue processing while the user leaves the GlowSync activity, as permitted.
- Maintain WLED communication.
- Provide a system-visible way to stop active synchronization.
- Release resources when the session ends.

Handle:

- TV sleep.
- Display mode changes.
- Wi-Fi disconnection.
- Network switching.
- WLED restart.
- MediaProjection revocation.
- Android process termination.
- Foreground-service restrictions.
- TV manufacturer limitations.

Avoid busy polling and excessive battery or power use.

If capture is lost, transition to a safe state and show that a new permission request may be needed.

---

# 13. SETTINGS

Create a premium settings interface containing:

**General**
- Launch preferences.
- Theme and motion settings.
- Language-ready architecture.

**WLED**
- Saved controllers.
- Network details.
- Reconnect.
- Test LEDs.
- Controller diagnostics.

**LED Layout**
- Mapping wizard.
- Calibration.
- Physical direction test.

**Capture**
- Permission status.
- Active session state.
- Supported-content explanation.

**Performance**
- FPS profiles.
- Analysis resolution.
- Smoothing.
- Diagnostic data.

**Privacy**
- Local-only processing explanation.
- No screenshots saved by default.
- Clear local data.

**About**
- GlowSync version.
- By Robonavigators.
- Open-source licenses and acknowledgements.
- Troubleshooting and support information.

---

# 14. PRIVACY AND SECURITY

All screen analysis must happen locally on the Google TV device.

Do not upload captured frames.

Do not require cloud accounts.

Do not require subscriptions or paid APIs.

Do not create unnecessary analytics or tracking systems.

Do not record video to disk.

Do not store or log screenshots.

Store saved device configuration locally using DataStore or another appropriate Android persistence mechanism.

Use secure networking practices for discovery and control operations.

Explain that WLED DDP streaming uses unencrypted local UDP and should remain on a trusted network.

Never implement DRM bypasses or unauthorized screen capture techniques.

---

# 15. PROJECT ARCHITECTURE

Use a clean, modular Kotlin architecture.

Suggested structure:

```text
app/
  src/main/
    java/com/robonavigators/glowsync/
      MainActivity.kt
      GlowSyncApplication.kt

      capture/
        ProjectionController.kt
        ProjectionForegroundService.kt
        FrameCapturePipeline.kt
        CaptureState.kt

      processing/
        EdgeSampler.kt
        ColorEngine.kt
        BlackBarDetector.kt
        SmoothingEngine.kt
        LedMapper.kt

      network/
        WledDiscovery.kt
        WledClient.kt
        DdpEncoder.kt
        DdpUdpStreamer.kt
        ConnectionMonitor.kt

      performance/
        PerformanceMonitor.kt
        AdaptiveQualityController.kt

      data/
        SettingsRepository.kt
        DeviceProfile.kt
        LedLayout.kt

      ui/
        theme/
        components/
        screens/
          SplashScreen.kt
          WelcomeScreen.kt
          DeviceDiscoveryScreen.kt
          LedSetupScreen.kt
          PermissionScreen.kt
          CalibrationScreen.kt
          DashboardScreen.kt
          SettingsScreen.kt
          DiagnosticsScreen.kt

      navigation/
        GlowSyncNavigation.kt

    AndroidManifest.xml
    res/

  src/test/
  src/androidTest/

README.md
```

You may refine the structure when justified, but maintain clear separation between UI, capture, processing and streaming.

Use up-to-date stable dependencies supported by the selected Android Gradle Plugin and SDK.

Prefer a minimal dependency footprint.

Avoid unnecessary native libraries unless performance measurements justify them.

---

# 16. TESTING REQUIREMENTS

Implement meaningful automated tests.

## DDP tests

Verify:

- Correct header encoding.
- Channel offsets.
- Payload length.
- RGB byte order.
- Sequence handling.
- Multi-packet splitting.
- Final packet PUSH flag.
- Large LED arrays.
- Zero-length and invalid configurations.

## LED mapping tests

Verify:

- Four-sided installation.
- Three-sided installation.
- All starting corners.
- Clockwise and counterclockwise.
- Unequal LED counts per side.
- No duplicate or skipped physical LED indices.
- Correct edge-to-LED ordering.

## Frame processing tests

Verify:

- Correct average colour extraction.
- Handling of row stride and pixel stride.
- Black-bar handling.
- Rotation/aspect ratio changes.
- Frame dropping.
- Smoothing consistency across different frame intervals.

## Android lifecycle tests

Verify:

- Permission accepted.
- Permission denied.
- Projection revoked.
- Capture stopped.
- Activity moved to background.
- Foreground service stopped.
- App process restarted.
- Wi-Fi disconnected.
- Device becomes unavailable.

Use mocks or fake frame sources for automated tests.

Do not claim physical-device testing unless it was actually performed.

---

# 17. HARDWARE VALIDATION

Primary target: consumer Google TV / Android TV hardware, including relatively low-powered devices.

Validate performance using real devices where available.

Provide a clear compatibility test.

Do not assume that every Android TV implements MediaProjection identically.

Document device-dependent limitations.

For WLED testing, use:

- An ESP32 running a supported WLED version.
- An addressable RGB LED strip.
- The TV and ESP32 on a reachable local network.

Build a standalone LED test mode that works even when screen capture is unavailable.

Document Wi-Fi stability requirements and how LED count affects the maximum achievable update rate.

---

# 18. BUILD, OUTPUT AND DOCUMENTATION

Deliver a complete Android Studio project.

Include:

1. Kotlin source code.
2. Gradle Kotlin DSL files.
3. Gradle wrapper.
4. Android manifest.
5. TV launcher activity and banner.
6. Custom GlowSync visual assets.
7. Compose TV UI.
8. Working MediaProjection integration.
9. Foreground service.
10. Working LED colour extraction.
11. Working DDP encoder and UDP transmitter.
12. Real WLED discovery and connectivity.
13. Setup wizard and local settings persistence.
14. Tests.
15. README.md.
16. Build and installation instructions.

Run available compilation and tests.

Produce a debug APK when the environment supports Android builds.

If the environment lacks an Android SDK, state exactly what could not be verified.

Do not create empty placeholder files and pretend they are implemented.

Do not provide only a design explanation.

Use real implementations.

## README contents

Document:

- GlowSync overview.
- By Robonavigators branding.
- Requirements.
- Supported Android versions.
- Permissions.
- WLED configuration.
- LED mapping.
- Build instructions.
- Installation using ADB.
- First-time setup.
- Performance profiles.
- Troubleshooting.
- DRM and secure-content limitations.
- Android lifecycle restrictions.
- Testing instructions.
- Privacy model.
- Known issues.

Update the README whenever features change.

---

# 19. DEVELOPMENT EXECUTION STRATEGY

First inspect the repository and identify existing code if present.

Preserve existing useful functionality.

Then implement the project in logical stages:

**Stage A:** Android TV project, theme, navigation, visual assets and setup UI.

**Stage B:** Real WLED discovery, controller configuration, DDP encoding and physical LED test mode.

**Stage C:** MediaProjection permissions, foreground service and frame capture.

**Stage D:** Low-latency edge sampling, colour engine and physical LED mapping.

**Stage E:** Full end-to-end synchronization, performance optimization and adaptive frame dropping.

**Stage F:** Diagnostics, automated tests, lifecycle robustness, documentation and APK build.

Do not stop after Stage A or produce only a frontend.

Continue through the implementation unless an actual technical blocker prevents progress.

When blocked, implement all independent components and report the specific blocker.

---

# 20. FINAL ACCEPTANCE CRITERIA

GlowSync is complete only when:

- It launches as a native TV app.
- The UI is professionally polished and D-pad navigable.
- The setup wizard persists configuration.
- The real Android capture permission dialog appears.
- Authorized and supported screen content can be captured.
- The app extracts edge colours accurately.
- Physical LEDs are mapped correctly.
- Real RGB data is streamed to WLED through DDP.
- Synchronization works during a permitted background capture session.
- The app handles capture denial and protected content safely.
- No frame-processing backlog accumulates.
- Performance metrics reflect real measurements.
- The app handles normal device and networking failures.
- Automated tests pass.
- README.md accurately documents the project.
- The project builds successfully wherever suitable tooling exists.

At the end, report:

- Files created or modified.
- Implemented features.
- Build and test results.
- Measured performance, if tested.
- Unverified device-dependent behaviour.
- Remaining limitations.
- APK output location, if generated.

**FINAL PRODUCT VISION**

GlowSync must feel like a premium, futuristic, commercially polished Google TV application.

It should have beautiful interactive glassmorphism animations, effortless WLED setup, intelligent LED calibration, genuine Android permission dialogs, and an extremely fast local screen-to-light processing pipeline.

The code should be engineered for actual use, not merely to look convincing in a demonstration.

**GlowSync — By Robonavigators.**

Build it with performance, precision, elegance, privacy, and reliability as first-class requirements.
