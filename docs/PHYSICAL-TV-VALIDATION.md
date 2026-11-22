# Physical TV + WLED validation checklist

GlowSync is in early development. Emulator tests and local UDP tests do **not** demonstrate that screen-to-LED synchronization works on an actual TV. Use this checklist to collect a reproducible hardware report before describing a setup as supported.

## Test equipment

- Android TV / Google TV model, Android version, firmware/build number.
- WLED version, ESP controller, LED type, total LED count and power supply rating.
- TV and WLED on a trusted local network; note whether they are on the same subnet.
- A non-DRM colour-bar or moving-colour test source shown **inside an Android app**.
- Optional high-speed video recording with both the TV screen and the strip visible.

## Safety and preparation

1. Confirm a correctly rated LED power supply, common ground where required, and safe wiring. Never power a long LED strip directly from a TV USB port unless its documented current rating suffices.
2. Keep the strip's brightness/power limit appropriate for its power supply.
3. Record the current WLED preset/settings, so the setup can be restored.
4. Never post Wi-Fi passwords, private IP addresses, ADB pairing codes or screen recordings containing personal information in a public issue.
5. If using ADB, authorize only a trusted computer and disable network debugging when finished.

## Functional checks

| Check | Procedure | Record |
|---|---|---|
| Controller discovery | Launch GlowSync and discover WLED on the local network | Found / not found |
| Manual connection | Enter WLED's IP/hostname and run verification | Verified / error |
| LED direction | Trigger the one-LED marker at each logical corner | Expected / reversed / offset |
| Three-sided strip | Set the bottom LED count to zero | Correct / incorrect |
| Screen capture | Grant the Android system screen-capture prompt | Allowed / denied / unavailable |
| Basic colours | Show red, green, blue, white and black **non-DRM** patterns | Expected / shifted / absent |
| Moving colours | Move a bright object across the four edges | Smooth / delayed / incorrect |
| Stop and restart | Stop sync, then initiate a new session with fresh consent | Pass / fail |
| Backgrounding | Press Home, reopen the app and inspect sync state | Observed behavior |
| Permission revocation | Revoke/stop capture using Android controls | Stops safely / issue |
| WLED interruption | Disconnect WLED temporarily, then reconnect it | Observed recovery |
| Screen sleep | Put the TV to sleep during synchronization | Stops safely / issue |

Do not disable TV security controls or attempt to bypass DRM/secure-window protection during testing. A black capture can have multiple causes; it is not evidence of any specific DRM behavior.

## Performance checks

- Log the requested FPS and the FPS actually observed.
- Capture any relevant GlowSync app-side diagnostics: frame processing, encoding, UDP call time, frame age and dropped-frame behavior.
- For **visible end-to-end latency**, record the TV and strip together and compare transitions. App-side UDP timing alone does not measure LED response.
- Observe temperature, stability and frame pacing during an extended session; note the duration and whether the TV remains responsive.
- Repeat with different LED counts or FPS targets if performance is inconsistent.

## Report template

Copy this into a GitHub issue, replacing placeholders. Do not publish identifying network information.

```text
TV brand/model:
Android TV / Google TV version:
Firmware/build:
GlowSync commit or release:
WLED version:
ESP controller:
LED chipset and LED count:
LED arrangement (top/right/bottom/left):
Network arrangement (same subnet / routed / unknown):
Discovery and verification:
One-LED marker result:
Capture permission and content source (non-DRM):
Colour and motion observations:
Restart/background/sleep behavior:
Measured visible latency (method and value, if measured):
App-reported diagnostics (if available):
Reproduction steps:
Expected vs actual behavior:
```

## Completion criteria

A device should not be described as **physically validated** until the core connection, mapping, capture, colour, stop and restart tests pass on that exact TV/WLED combination. Document limitations and failed cases rather than generalizing from one configuration. Emulator-only findings must remain labelled as emulator tests.

See [Troubleshooting](TROUBLESHOOTING.md), [WLED](wled.md), [Capture](capture.md), and [Device reports](devices/README.md) for related guidance.
