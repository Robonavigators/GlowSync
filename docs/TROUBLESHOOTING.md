# Troubleshooting GlowSync

## WLED does not appear

Ensure the TV and WLED controller can reach each other. Multicast discovery can be blocked by Wi-Fi isolation or router configuration; enter the controller's IP or hostname manually and use **Test connection**. A successful `/json/info` response verifies HTTP reachability, not DDP delivery. Check WLED's real-time configuration, UDP port 4048, LED count and local firewall. Use a trusted network: WLED HTTP and DDP are unencrypted.

## LEDs are in the wrong place

Set counts for top, right, bottom and left; use zero for an absent edge. With sync stopped, send the one-LED marker and step through the strip. Adjust starting corner, offset and reverse until the physical path agrees with the preview. A UDP test frame is not an acknowledgement; inspect the strip itself.

## Screen is black or capture stops

Android may prevent capture of protected video, secure windows and HDMI inputs. A dark scene and blocked content may look alike. Select a dark-frame fallback if appropriate, or use the standalone LED test. If projection is stopped, revoked or the TV sleeps, start a new session and accept the system prompt. Android TV implementations vary; test on the actual device.

## Flicker or lag

Try Balanced or Eco, a lower FPS cap, and a stable local network. Increase smoothing for slower transitions or choose Ultra Fast for minimal smoothing. Large strips need multiple DDP packets. The app's processing and UDP-call timings are not physical LED latency; see the README's external measurement procedure.

## Build or install fails

Install JDK 17 or newer and Android SDK 35, then run `./gradlew testDebugUnitTest lintDebug assembleDebug`. Confirm ADB sees an authorized device with `adb devices` before installing. Android Studio can manage the SDK and open the project directly.
