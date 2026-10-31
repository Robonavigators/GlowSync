# AGENTS.md — GlowSync agent and contributor engineering guide

Instructions for Codex, Claude Code, Gemini CLI, Cursor, Copilot and other coding agents working on **GlowSync by Robonavigators**. Human contributors should also read [README.md](README.md), [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

**Source of truth:** this file defines shared agent rules. `CLAUDE.md`, `GEMINI.md` and `CODEX.md` are entry points, not independent policies. The current implementation and tested limitations are documented in [README.md](README.md); [PROMPT.md](PROMPT.md) is a product vision, **not** proof of implementation.

## First: what job are you doing?

| Requested task | Agent workflow |
|---|---|
| Fix a bug or add functionality | Locate the smallest responsible module, inspect tests and call sites, make a focused change, add regression coverage, run relevant checks, update user-facing docs |
| Improve UI or accessibility | Preserve D-pad navigation, focus, contrast, reduced motion and TV performance; validate navigation and supply non-sensitive screenshots if possible |
| Report TV or WLED compatibility | Use existing [device-report guidance](docs/devices/README.md); distinguish real hardware from emulator and never invent successful tests |
| Improve documentation or translations | Keep links and capability claims consistent, preserve official branding, run applicable checks |
| Investigate latency or performance | Define the measurement boundary, collect actual timings, compare before and after; avoid guessing screen-to-light latency |
| Handle security or privacy | Follow [SECURITY.md](SECURITY.md); never publish secrets or captured frames; do not weaken consent or DRM protections |
| Package or release | Confirm tests and reproducible artifacts, give truthful installation and known-limitations notes; never label an untested preview production-ready |

## Non-negotiable safeguards

1. **Consent and protected content:** respect Android MediaProjection permission and token lifecycle, foreground-service rules and secure-window / DRM / HDMI restrictions. No silent or background bypass of consent.
2. **Capture privacy:** process permitted frames locally; never upload, persist, print or attach captured screen contents. No cloud dependency added without explicit project approval.
3. **Streaming integrity:** use latest-frame-wins behavior and bounded resources. Close acquired images, avoid unnecessary full-resolution copies, and never queue stale frames to reach a target FPS.
4. **Network honesty:** WLED discovery and verification do not prove LEDs illuminated. HTTP/DDP traffic is not encrypted; do not claim otherwise. Treat local addresses and device IDs as potentially sensitive.
5. **Safety in repo operations:** do not commit credentials, keystores, signing keys, local configurations, screen recordings or private device/network details. No destructive or broad refactor unrelated to the request.
6. **Performance claims:** measure each stage with appropriate timing; UDP send completion is not visual output latency. Report limitations and measurement environments accurately.
7. **TV-first experience:** preserve usable remote-only navigation, visible focus, stable layout, legible text and reduced-motion behavior. Animations must not interfere with capture.
8. **Maintain documentation:** whenever behavior, installation, compatibility, privacy, architecture or limitations change, update `README.md` and the relevant `docs/` page in the same change. Preserve the [GlowSync website](https://robonavigators-io.vercel.app/GlowSync/) and [Robonavigators website](https://robonavigators-io.vercel.app/) links.

## Where changes belong

| Area | Location |
|---|---|
| Capture and MediaProjection lifecycle | `app/src/main/java/com/robonavigators/glowsync/capture/` |
| Edge sampling, correction and smoothing | `app/src/main/java/com/robonavigators/glowsync/processing/` |
| WLED discovery, verification and DDP | `app/src/main/java/com/robonavigators/glowsync/network/` |
| Adaptive frame rate and resolution | `app/src/main/java/com/robonavigators/glowsync/performance/` |
| Measurement and diagnostic test patterns | `app/src/main/java/com/robonavigators/glowsync/diagnostics/` |
| Android unit and instrumentation tests | `app/src/test/`, `app/src/androidTest/` |
| Hardware reports and technical guides | `docs/`, `docs/devices/` |
| Optional examples, tools, themes and add-ons | `examples/`, `tools/`, `themes/`, `addons/` (follow existing interfaces; do not invent a plugin API) |
| Public overview and contributor process | `README.md`, `CONTRIBUTING.md` |

## Before editing

- Read the user request and relevant repository instructions. Inspect the existing implementation before introducing new files or frameworks.
- Prefer the smallest self-contained change. Preserve compatibility and observable behavior outside the requested scope.
- Check project dependencies and Gradle configuration rather than assuming a build target or API level.
- For community PRs, describe the user-visible problem, implementation, tests, limitations and affected devices.

## Validation and reporting

Use **JDK 17+ and Android SDK 35** as documented, then run applicable commands:

```sh
python3 -m unittest discover -s tests -v
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest  # only with a compatible device/emulator
```

- For code changes, start with focused tests, then run the available broader checks; do not mark unrun checks as passed.
- Physical TV and WLED tests must be identified by actual models/firmware/LED layout. Emulator success is **not** physical-hardware validation.
- If a test is blocked, report the reason and the remaining risk rather than inventing a result.
- End every completed task with a concise summary of files changed, checks run/results, known caveats, and a link to the PR or commit when one exists.

## Contributing and review

- Preserve the MIT license, contributor attribution and existing public documentation.
- Prefer PRs for externally contributed changes; do not merge changes without reviewing their diff and test evidence.
- Keep unrelated work out of one PR. Flag safety/privacy regressions and undocumented API or dependency changes.
- Follow [CONTRIBUTING.md](CONTRIBUTING.md) for human contributor instructions and [SECURITY.md](SECURITY.md) for private vulnerability reporting.

---

## Existing technical implementation constraints

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
