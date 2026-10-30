# Architecture

The Android project lives at the repository root; `app/` is the application module. The main path is MediaProjection → VirtualDisplay → low-resolution ImageReader → CPU edge sampling and colour adjustment → LED mapping → RGB24 DDP → UDP → WLED. The foreground service owns the projection and capture worker; a serialized network worker transmits the newest pending frame. Controller discovery and HTTP status checks are separate from high-speed streaming.

| Location | Responsibility |
|---|---|
| `app/src/main/java/com/robonavigators/glowsync/capture` | Consent session, foreground service, frame acquisition, cleanup and metrics. |
| `processing` | Sampling, mapping, smoothing, black-bar and dark-frame handling. |
| `network` | WLED discovery and verification, DDP packets and UDP sender. |
| `performance` | Adaptive output-rate and resolution policies. |
| `diagnostics` | Direct-UDP reference pattern, not capture-to-LED timing. |
| `data` | Locally stored controller metadata. |
| `app/src/test`, `app/src/androidTest` | JVM and device/emulator tests. |

See [AGENTS.md](../AGENTS.md) for constraints and [README.md](../README.md) for limitations.
